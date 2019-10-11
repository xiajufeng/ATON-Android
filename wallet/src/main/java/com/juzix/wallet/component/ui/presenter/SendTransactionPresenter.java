package com.juzix.wallet.component.ui.presenter;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.juzhen.framework.network.ApiRequestBody;
import com.juzhen.framework.network.ApiResponse;
import com.juzhen.framework.network.ApiSingleObserver;
import com.juzhen.framework.network.NetConnectivity;
import com.juzhen.framework.util.LogUtils;
import com.juzhen.framework.util.NumberParserUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.app.CustomObserver;
import com.juzix.wallet.app.CustomThrowable;
import com.juzix.wallet.app.LoadingTransformer;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.SendTransationContract;
import com.juzix.wallet.component.ui.dialog.InputWalletPasswordDialogFragment;
import com.juzix.wallet.component.ui.dialog.SendTransactionDialogFragment;
import com.juzix.wallet.component.ui.dialog.TransactionAuthorizationDialogFragment;
import com.juzix.wallet.component.ui.dialog.TransactionSignatureDialogFragment;
import com.juzix.wallet.component.ui.view.AssetsFragment;
import com.juzix.wallet.component.ui.view.MainActivity;
import com.juzix.wallet.db.entity.AddressEntity;
import com.juzix.wallet.db.sqlite.AddressDao;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.engine.ServerUtils;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.engine.TransactionManager;
import com.juzix.wallet.engine.Web3jManager;
import com.juzix.wallet.entity.AccountBalance;
import com.juzix.wallet.entity.PlatOnFunction;
import com.juzix.wallet.entity.Transaction;
import com.juzix.wallet.entity.TransactionAuthorizationBaseData;
import com.juzix.wallet.entity.TransactionAuthorizationData;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.AddressFormatUtil;
import com.juzix.wallet.utils.BigDecimalUtil;
import com.juzix.wallet.utils.JZWalletUtil;
import com.juzix.wallet.utils.RxUtils;
import com.juzix.wallet.utils.ToastUtil;
import com.trello.rxlifecycle2.android.FragmentEvent;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.platon.FunctionType;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;


/**
 * @author matrixelement
 */
public class SendTransactionPresenter extends BasePresenter<SendTransationContract.View> implements SendTransationContract.Presenter {

    private final static double DEFAULT_PERCENT = 0;
    //默认gasLimit
    private final static BigInteger DEAULT_GAS_LIMIT = DefaultGasProvider.GAS_LIMIT;
    //默认最小gasPrice
    private final static BigInteger DEFAULT_MIN_GASPRICE = DefaultGasProvider.GAS_PRICE;
    //默认最大gasPrice
    private final static BigInteger DEFAULT_MAX_GASPRICE = BigInteger.valueOf(6).multiply(DefaultGasProvider.GAS_PRICE);
    //当前gasLimit
    private BigInteger gasLimit = DEAULT_GAS_LIMIT;
    //最低gasPrice
    private BigInteger minGasPrice = DEFAULT_MIN_GASPRICE;
    //最高gasPrice
    private BigInteger maxGasPrice = DEFAULT_MAX_GASPRICE;
    //最高与最低差值
    private BigInteger dGasPrice = maxGasPrice.subtract(minGasPrice);
    //当前gasPrice
    private BigInteger gasPrice = minGasPrice;
    //当前滑动百分比
    private double percent = DEFAULT_PERCENT;

    private double feeAmount;
    //刷新时间
    private Disposable mDisposable;
    private Wallet walletEntity;
    private String toAddress;

    public SendTransactionPresenter(SendTransationContract.View view) {
        super(view);
    }

    @Override
    public void init() {
        if (isViewAttached()) {
            if (!TextUtils.isEmpty(toAddress)) {
                getView().setToAddress(toAddress);
            }
        }
    }

    public String getSenderAddress() {
        return walletEntity != null ? walletEntity.getPrefixAddress() : "";
    }

    @Override
    public void fetchDefaultWalletInfo() {
        walletEntity = WalletManager.getInstance().getSelectedWallet();
        if (walletEntity == null) {
            return;
        }

        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }

        ServerUtils
                .getCommonApi()
                .getAccountBalance(ApiRequestBody.newBuilder()
                        .put("addrs", Arrays.asList(walletEntity.getPrefixAddress()))
                        .build())
                .compose(bindUntilEvent(FragmentEvent.STOP))
                .compose(RxUtils.getSingleSchedulerTransformer())
                .subscribe(new ApiSingleObserver<List<AccountBalance>>() {

                    @Override
                    public void onApiSuccess(List<AccountBalance> accountBalances) {
                        if (isViewAttached() && accountBalances != null && !accountBalances.isEmpty()) {
                            getView().updateWalletBalance(accountBalances.get(0).getShowFreeBalace());
                        }
                    }

                    @Override
                    public void onApiFailure(ApiResponse response) {

                    }
                });

        mDisposable = Web3jManager
                .getInstance()
                .getGasPrice()
                .compose(RxUtils.getSingleSchedulerTransformer())
                .subscribe(new Consumer<BigInteger>() {
                    @Override
                    public void accept(BigInteger bigInteger) throws Exception {
                        if (isViewAttached()) {
                            minGasPrice = bigInteger;
                            maxGasPrice = bigInteger.multiply(BigInteger.valueOf(6));
                            dGasPrice = maxGasPrice.subtract(minGasPrice);
                            LogUtils.e("gasPrice为：  " + bigInteger.longValue() + "minGasPrice为： " + minGasPrice);
                            calculateFeeAndTime(percent);
                        }
                    }
                });
    }


    @Override
    public void transferAllBalance() {
        if (isViewAttached() && walletEntity != null) {
            if (BigDecimalUtil.isBigger(String.valueOf(feeAmount), walletEntity.getFreeBalance())) {
                getView().setTransferAmount(0D);
            } else {
                getView().setTransferAmount(BigDecimalUtil.sub(NumberParserUtils.getPrettyBalance(BigDecimalUtil.div(walletEntity.getFreeBalance(), "1E18")), String.valueOf(feeAmount)));
            }
        }
    }

    @Override
    public void calculateFee() {
        String toAddress = getView().getToAddress();
        String address = walletEntity.getPrefixAddress();
        if (TextUtils.isEmpty(toAddress) || walletEntity == null || TextUtils.isEmpty(address)) {
            return;
        }
        updateFeeAmount(percent);
    }

    @Override
    public void calculateFeeAndTime(double percent) {
        this.percent = percent;
        updateGasPrice(percent);
        updateFeeAmount(percent);
    }

    @Override
    public boolean checkToAddress(String toAddress) {
        String errMsg = null;
        if (TextUtils.isEmpty(toAddress)) {
            errMsg = string(R.string.address_cannot_be_empty);
        } else {
            if (!WalletUtils.isValidAddress(toAddress)) {
                errMsg = string(R.string.receive_address_error);
            }
        }
        getView().showToAddressError(errMsg);
        return TextUtils.isEmpty(errMsg);
    }

    @Override
    public boolean checkTransferAmount(String transferAmount) {

        String errMsg = null;

        if (TextUtils.isEmpty(transferAmount)) {
            errMsg = string(R.string.transfer_amount_cannot_be_empty);
        } else {
            if (!isBalanceEnough(transferAmount)) {
                errMsg = string(R.string.insufficient_balance);
            }
        }

        getView().showAmountError(errMsg);

        return TextUtils.isEmpty(errMsg);
    }

    @Override
    public void submit() {
        if (isViewAttached()) {
            if (!NetConnectivity.getConnectivityManager().isConnected()) {
                showLongToast(R.string.network_error);
                return;
            }
            String transferAmount = getView().getTransferAmount();
            String toAddress = getView().getToAddress();
            if (!checkToAddress(toAddress)) {
                return;
            }
            if (!checkTransferAmount(transferAmount)) {
                return;
            }
            String address = walletEntity.getPrefixAddress();
            if (toAddress.equals(address)) {
                showLongToast(R.string.can_not_send_to_itself);
                return;
            }

            String fromWallet = String.format("%s(%s)", walletEntity.getName(), AddressFormatUtil.formatTransactionAddress(walletEntity.getPrefixAddress()));
            String fee = NumberParserUtils.getPrettyBalance(feeAmount);

            getWalletNameFromAddress(toAddress)
                    .compose(RxUtils.getSingleSchedulerTransformer())
                    .compose(bindToLifecycle())
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            if (isViewAttached()) {
                                SendTransactionDialogFragment
                                        .newInstance(string(R.string.send_transaction), NumberParserUtils.getPrettyBalance(transferAmount), buildSendTransactionInfo(fromWallet, s, fee))
                                        .setOnConfirmBtnClickListener(new SendTransactionDialogFragment.OnConfirmBtnClickListener() {
                                            @Override
                                            public void onConfirmBtnClick() {
                                                if (WalletManager.getInstance().getSelectedWallet().isObservedWallet()) {
                                                    showTransactionAuthorizationDialogFragment(Convert.toVon(transferAmount, Convert.Unit.LAT).toPlainString(), walletEntity.getPrefixAddress(), toAddress, gasLimit.toString(10), gasPrice.toString(10));
                                                } else {
                                                    showInputWalletPasswordDialogFragment(transferAmount, fee, toAddress);
                                                }
                                            }
                                        })
                                        .show(currentActivity().getSupportFragmentManager(), "sendTransaction");
                            }
                        }
                    });
        }
    }

    @Override
    public void updateSendTransactionButtonStatus() {
        if (isViewAttached()) {
            String transferAmount = getView().getTransferAmount();
            String toAddress = getView().getToAddress();
            boolean isToAddressFormatCorrect = !TextUtils.isEmpty(toAddress) && WalletUtils.isValidAddress(toAddress);
            boolean isTransferAmountValid = !TextUtils.isEmpty(transferAmount) && NumberParserUtils.parseDouble(transferAmount) > 0 && isBalanceEnough(transferAmount);
            getView().setSendTransactionButtonEnable(isToAddressFormatCorrect && isTransferAmountValid);
        }

    }

    @Override
    public void saveWallet(String name, String address) {
        String[] avatarArray = getContext().getResources().getStringArray(R.array.wallet_avatar);
        String avatar = avatarArray[new Random().nextInt(avatarArray.length)];
        boolean success = AddressDao.insertAddressInfo(new AddressEntity(UUID.randomUUID().toString(), address, name, avatar));
        getView().setSaveAddressButtonEnable(!success);
        if (success) {
            showLongToast(string(R.string.save_successfully));
        }
    }

    @Override
    public void updateAssetsTab(int tabIndex) {
        if (isViewAttached()) {
            if (tabIndex != AssetsFragment.TAB2) {
                resetData();
                getView().resetView(BigDecimalUtil.parseString(feeAmount));
            }
        }
    }

    @Override
    public void checkAddressBook(String address) {
        if (TextUtils.isEmpty(address)) {
            getView().setSaveAddressButtonEnable(false);
            return;
        }
        if (JZWalletUtil.isValidAddress(address)) {
            getView().setSaveAddressButtonEnable(!AddressDao.isExist(address));
        } else {
            getView().setSaveAddressButtonEnable(false);
        }
    }

    @SuppressLint("CheckResult")
    private void sendTransaction(String privateKey, BigDecimal transferAmount, BigDecimal feeAmount, String toAddress) {
        TransactionManager
                .getInstance()
                .sendTransaction(privateKey, walletEntity.getPrefixAddress(), toAddress, walletEntity.getName(), transferAmount, feeAmount, gasPrice, gasLimit)
                .compose(RxUtils.getSingleSchedulerTransformer())
                .compose(LoadingTransformer.bindToSingleLifecycle(currentActivity()))
                .compose(bindToLifecycle())
                .subscribe(new Consumer<Transaction>() {
                    @Override
                    public void accept(Transaction transaction) {
                        if (isViewAttached()) {
                            showLongToast(string(R.string.transfer_succeed));
                            backToTransactionListWithDelay();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        if (isViewAttached()) {
                            if (throwable instanceof CustomThrowable) {
                                CustomThrowable exception = (CustomThrowable) throwable;
                                if (exception.getErrCode() == CustomThrowable.CODE_ERROR_TRANSFER_FAILED) {
                                    showLongToast(string(R.string.transfer_failed));
                                }
                            }
                        }
                    }
                });
    }

    /**
     * 延迟指定时间后返回交易列表页
     */
    private void backToTransactionListWithDelay() {
        Single
                .timer(1000, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (isViewAttached()) {
                            resetData();
                            getView().resetView(BigDecimalUtil.parseString(feeAmount));
                            MainActivity.actionStart(getContext(), MainActivity.TAB_PROPERTY, AssetsFragment.TAB1);
                        }
                    }
                });
    }

    private Single<Credentials> checkBalance(Wallet walletEntity, Credentials credentials, BigInteger gasPrice) {

        return Single.create(new SingleOnSubscribe<Credentials>() {
            @Override
            public void subscribe(SingleEmitter<Credentials> emitter) throws Exception {
                double balance = Web3jManager.getInstance().getBalance(walletEntity.getPrefixAddress());
                if (balance < feeAmount) {
                    emitter.onError(new CustomThrowable(CustomThrowable.CODE_ERROR_NOT_SUFFICIENT_BALANCE));
                } else {
                    emitter.onSuccess(credentials);
                }
            }
        });
    }


    private void showInputWalletPasswordDialogFragment(String transferAmount, String feeAmount, String toAddress) {
        InputWalletPasswordDialogFragment.newInstance(walletEntity).setOnWalletPasswordCorrectListener(new InputWalletPasswordDialogFragment.OnWalletPasswordCorrectListener() {
            @Override
            public void onWalletPasswordCorrect(Credentials credentials) {
                sendTransaction(Numeric.toHexStringNoPrefix(credentials.getEcKeyPair().getPrivateKey()), Convert.toVon(transferAmount, Convert.Unit.LAT), Convert.toVon(feeAmount, Convert.Unit.LAT), toAddress);
            }
        }).show(currentActivity().getSupportFragmentManager(), "inputPassword");
    }

    private void showTransactionAuthorizationDialogFragment(String transferAmount, String from, String to, String gasLimit, String gasPrice) {

        Observable
                .fromCallable(new Callable<BigInteger>() {
                    @Override
                    public BigInteger call() throws Exception {
                        return Web3jManager.getInstance().getNonce(from);
                    }
                })
                .compose(RxUtils.getSchedulerTransformer())
                .compose(bindToLifecycle())
                .compose(RxUtils.getLoadingTransformer(currentActivity()))
                .subscribe(new CustomObserver<BigInteger>() {
                    @Override
                    public void accept(BigInteger nonce) {
                        if (isViewAttached()) {
                            TransactionAuthorizationData transactionAuthorizationData = new TransactionAuthorizationData(Arrays.asList(new TransactionAuthorizationBaseData.Builder(FunctionType.TRANSFER)
                                    .setAmount(transferAmount)
                                    .setChainId(NodeManager.getInstance().getChainId())
                                    .setNonce(nonce.toString(10))
                                    .setFrom(from)
                                    .setTo(to)
                                    .setGasLimit(gasLimit)
                                    .setGasPrice(gasPrice)
                                    .build()), System.currentTimeMillis() / 1000);
                            TransactionAuthorizationDialogFragment.newInstance(transactionAuthorizationData)
                                    .setOnNextBtnClickListener(new TransactionAuthorizationDialogFragment.OnNextBtnClickListener() {
                                        @Override
                                        public void onNextBtnClick() {
                                            TransactionSignatureDialogFragment.newInstance(transactionAuthorizationData.getTimeStamp())
                                                    .setOnSendTransactionSucceedListener(new TransactionSignatureDialogFragment.OnSendTransactionSucceedListener() {
                                                        @Override
                                                        public void onSendTransactionSucceed(String hash) {
                                                            if (isViewAttached()) {
                                                                backToTransactionListWithDelay();
                                                            }
                                                        }
                                                    })
                                                    .show(currentActivity().getSupportFragmentManager(), TransactionSignatureDialogFragment.TAG);
                                        }
                                    })
                                    .show(currentActivity().getSupportFragmentManager(), "showTransactionAuthorizationDialog");
                        }
                    }

                    @Override
                    public void accept(Throwable throwable) {
                        super.accept(throwable);
                        if (isViewAttached()) {
                            if (!TextUtils.isEmpty(throwable.getMessage())) {
                                ToastUtil.showLongToast(currentActivity(), throwable.getMessage());
                            }
                        }
                    }
                });
    }

    private void updateFeeAmount(double percent) {
        double minFee = getMinFee();
        double maxFee = getMaxFee();
        double dValue = maxFee - minFee;
        feeAmount = BigDecimalUtil.add(minFee, BigDecimalUtil.mul(percent, dValue), 8, RoundingMode.CEILING);
        if (isViewAttached()) {
            getView().setTransferFeeAmount(BigDecimalUtil.parseString(feeAmount));
        }
    }

    private void updateGasPrice(double percent) {
        gasPrice = minGasPrice.add(BigDecimalUtil.mul(String.valueOf(percent), String.valueOf(dGasPrice.doubleValue())).toBigInteger());
        LogUtils.e("当前gasPrice为：" + gasPrice);
    }

    private boolean isBalanceEnough(String transferAmount) {
        double usedAmount = BigDecimalUtil.add(NumberParserUtils.parseDouble(transferAmount), feeAmount);
        if (walletEntity != null) {
            return BigDecimalUtil.isBigger(walletEntity.getFreeBalance(), String.valueOf(usedAmount));
        }
        return false;
    }

    private double getMinFee() {
        return BigDecimalUtil.div(BigDecimalUtil.mul(gasLimit.doubleValue(), minGasPrice.doubleValue()), 1E18);
    }

    private double getMaxFee() {
        return BigDecimalUtil.div(BigDecimalUtil.mul(gasLimit.doubleValue(), maxGasPrice.doubleValue()), 1E18);
    }

    private Map<String, String> buildSendTransactionInfo(String fromWallet, String recipient, String fee) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(String.format("%s:", string(R.string.txt_info)), string(R.string.send_energon));
        map.put(String.format("%s:", string(R.string.from_wallet)), fromWallet);
        map.put(String.format("%s:", string(R.string.recipient_address)), recipient);
        map.put(String.format("%s:", string(R.string.fee)), string(R.string.amount_with_unit, fee));
        return map;
    }

    private void resetData() {
        toAddress = "";
        gasPrice = minGasPrice;
        gasLimit = DEAULT_GAS_LIMIT;
        percent = DEFAULT_PERCENT;
        feeAmount = getMinFee();
    }

    private Single<String> getWalletNameFromAddress(String address) {
        return Single.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String walletName = WalletManager.getInstance().getWalletNameByWalletAddress(address);
                return TextUtils.isEmpty(walletName) ? walletName : String.format("%s(%s)", walletName, AddressFormatUtil.formatTransactionAddress(address));
            }
        }).filter(new Predicate<String>() {
            @Override
            public boolean test(String s) throws Exception {
                return !TextUtils.isEmpty(s);
            }
        }).switchIfEmpty(new SingleSource<String>() {
            @Override
            public void subscribe(SingleObserver<? super String> observer) {
                String addressName = AddressDao.getAddressNameByAddress(address);
                observer.onSuccess(TextUtils.isEmpty(addressName) ? addressName : String.format("%s(%s)", addressName, AddressFormatUtil.formatTransactionAddress(address)));
            }
        }).filter(new Predicate<String>() {
            @Override
            public boolean test(String s) throws Exception {
                return !TextUtils.isEmpty(s);
            }
        }).defaultIfEmpty(AddressFormatUtil.formatAddress(address))
                .toSingle();
    }

}