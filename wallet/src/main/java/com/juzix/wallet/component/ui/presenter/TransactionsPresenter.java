package com.juzix.wallet.component.ui.presenter;


import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import com.juzhen.framework.network.ApiErrorCode;
import com.juzhen.framework.network.ApiRequestBody;
import com.juzhen.framework.network.ApiResponse;
import com.juzhen.framework.util.LogUtils;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.TransactionsContract;
import com.juzix.wallet.db.entity.TransactionEntity;
import com.juzix.wallet.db.sqlite.TransactionDao;
import com.juzix.wallet.engine.ServerUtils;
import com.juzix.wallet.engine.TransactionManager;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.entity.Transaction;
import com.juzix.wallet.entity.TransactionStatus;
import com.juzix.wallet.utils.RxUtils;
import com.trello.rxlifecycle2.android.FragmentEvent;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;


/**
 * @author matrixelement
 */
public class TransactionsPresenter extends BasePresenter<TransactionsContract.View> implements TransactionsContract.Presenter {

    private static final String TAG = TransactionsPresenter.class.getSimpleName();

    public final static String DIRECTION_OLD = "old";
    public final static String DIRECTION_NEW = "new";

    private Map<String, List<Transaction>> mTransactionMap = new HashMap<>();

    private Disposable mAutoRefreshDisposable;
    private Disposable mLoadLatestDisposable;
    private String mWalletAddress;

    public TransactionsPresenter(TransactionsContract.View view) {
        super(view);
        mAutoRefreshDisposable = new CompositeDisposable();
        mLoadLatestDisposable = new CompositeDisposable();
    }


    /**
     * 加载最新的数据，切换钱包时
     */
    @SuppressLint("CheckResult")
    @Override
    public void loadLatestData() {

        mWalletAddress = WalletManager.getInstance().getSelectedWalletAddress();

        if (TextUtils.isEmpty(mWalletAddress)) {
            return;
        }

        if (!mLoadLatestDisposable.isDisposed()) {
            mLoadLatestDisposable.dispose();
        }

        if (!mAutoRefreshDisposable.isDisposed()) {
            mAutoRefreshDisposable.dispose();
        }

        mLoadLatestDisposable = getTransactionList(mWalletAddress)
                .toObservable()
                .observeOn(Schedulers.newThread())
                .compose(RxUtils.getSchedulerTransformer())
                .subscribe(new Consumer<List<Transaction>>() {
                    @Override
                    public void accept(List<Transaction> transactionList) {
                        if (isViewAttached()) {
                            //先进行排序
                            List<Transaction> transactions = mTransactionMap.get(mWalletAddress);
                            List<Transaction> newTransactionList = getNewTransactionList(transactions, transactionList, true);
                            Collections.sort(newTransactionList);
                            getView().notifyDataSetChanged(transactions, newTransactionList, mWalletAddress, true);

                            mTransactionMap.put(mWalletAddress, newTransactionList);

                            deleteExceptionalTransaction(transactions, newTransactionList);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "loadLatestData onApiFailure");
                        if (isViewAttached()) {
                            getView().notifyDataSetChanged(mTransactionMap.get(mWalletAddress), mTransactionMap.get(mWalletAddress), mWalletAddress, true);
                        }
                    }
                });
    }

    @SuppressLint("CheckResult")
    @Override
    public void loadNew(String direction) {

        mWalletAddress = WalletManager.getInstance().getSelectedWalletAddress();

        if (TextUtils.isEmpty(mWalletAddress)) {
            return;
        }

        if (!mAutoRefreshDisposable.isDisposed()) {
            mAutoRefreshDisposable.dispose();
        }

        mAutoRefreshDisposable = getTransactionListWithTime(mWalletAddress, direction)
                .toObservable()
                .compose(RxUtils.getSchedulerTransformer())
                .compose(RxUtils.bindToParentLifecycleUtilEvent(getView(), FragmentEvent.STOP))
                .subscribe(new Consumer<List<Transaction>>() {
                    @Override
                    public void accept(List<Transaction> transactionList) throws Exception {
                        if (isViewAttached()) {
                            //先进行排序
                            List<Transaction> transactions = mTransactionMap.get(mWalletAddress);
                            List<Transaction> newTransactionList = getNewTransactionList(transactions, transactionList, true);
                            Collections.sort(newTransactionList);
                            getView().notifyDataSetChanged(transactions, newTransactionList, mWalletAddress, false);

                            mTransactionMap.put(mWalletAddress, newTransactionList);

                            deleteExceptionalTransaction(transactions, newTransactionList);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "onApiFailure");
                        if (isViewAttached()) {
                            getView().notifyDataSetChanged(mTransactionMap.get(mWalletAddress), mTransactionMap.get(mWalletAddress), mWalletAddress, true);
                        }
                    }
                });
    }

    @Override
    public void deleteTransaction(Transaction transaction) {

        List<Transaction> transactionList = mTransactionMap.get(mWalletAddress);
        if (transactionList != null && !transactionList.isEmpty()) {
            List<Transaction> newTransactionList = new ArrayList<>(transactionList);
            newTransactionList.remove(transaction);
            Collections.sort(newTransactionList);
            getView().notifyDataSetChanged(transactionList, newTransactionList, mWalletAddress, false);

            mTransactionMap.put(mWalletAddress, newTransactionList);
        }
    }

    @Override
    public synchronized void addNewTransaction(Transaction transaction) {
        if (isViewAttached()) {
            List<Transaction> transactionList = mTransactionMap.get(mWalletAddress);
            List<Transaction> newTransactionList = new ArrayList<>();
            if (isCurrentSelectedWallet(transaction)) {
                if (transactionList != null && !transactionList.isEmpty()) {
                    if (transactionList.contains(transaction)) {
                        //更新
                        newTransactionList = new ArrayList<>(transactionList);
                        int index = newTransactionList.indexOf(transaction);
                        newTransactionList.set(index, transaction);
                        Collections.sort(newTransactionList);
                        getView().notifyDataSetChanged(transactionList, newTransactionList, mWalletAddress, false);
                    } else {
                        //添加
                        newTransactionList = getNewTransactionList(transactionList, Arrays.asList(transaction), true);
                        Collections.sort(newTransactionList);
                        getView().notifyDataSetChanged(transactionList, newTransactionList, mWalletAddress, false);
                    }
                    mTransactionMap.put(mWalletAddress, newTransactionList);
                }
            }

        }
    }

    /**
     * 当前交易
     *
     * @param transaction
     * @return
     */
    private boolean isCurrentSelectedWallet(Transaction transaction) {
        return transaction.getFrom().equals(mWalletAddress) || transaction.getTo().equals(mWalletAddress);
    }

    /**
     * 删除异常的交易记录，删除的前提就是，服务端返回了与本地异常(交易状态为pending或者timeout)交易记录相同的记录
     * 如果服务端
     *
     * @param oldTransactionList
     * @param curTransactionList
     */
    private void deleteExceptionalTransaction(List<Transaction> oldTransactionList, List<Transaction> curTransactionList) {

        if (oldTransactionList == null || oldTransactionList.isEmpty() || curTransactionList.isEmpty()) {
            return;
        }

        for (int i = 0; i < oldTransactionList.size(); i++) {
            Transaction oldTransaction = oldTransactionList.get(i);
            boolean isOldTransactionStatusException = oldTransaction.getTxReceiptStatus() == TransactionStatus.PENDING || oldTransaction.getTxReceiptStatus() == TransactionStatus.TIMEOUT;
            if (curTransactionList.contains(oldTransaction) && isOldTransactionStatusException) {
                Transaction newTransaction = curTransactionList.get(i);
                if (newTransaction.getTxReceiptStatus() == TransactionStatus.SUCCESSED || newTransaction.getTxReceiptStatus() == TransactionStatus.FAILED) {
                    //删除掉
                    deleteTransaction(oldTransactionList.get(i).getHash());
                }
            }
        }

    }

    private void deleteTransaction(String hash) {
        Single
                .fromCallable(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return TransactionDao.deleteTransaction(hash);
                    }
                })
                .subscribeOn(Schedulers.io())
                .filter(new Predicate<Boolean>() {
                    @Override
                    public boolean test(Boolean aBoolean) throws Exception {
                        return aBoolean;
                    }
                })
                .doOnSuccess(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        LogUtils.e("deleteTransaction cancelTaskByHash " + hash);
                        TransactionManager.getInstance().cancelTaskByHash(hash);
                    }
                })
                .subscribe();
    }

    private List<Transaction> getNewTransactionList(List<Transaction> oldTransactionList, List<Transaction> curTransactionList, boolean isLoadMore) {
        List<Transaction> oldList = oldTransactionList == null ? new ArrayList<Transaction>() : oldTransactionList;
        List<Transaction> curList = curTransactionList;
        List<Transaction> newList = new ArrayList<>();
        if (isLoadMore) {
            newList.addAll(oldList);
            newList.removeAll(curList);
            newList.addAll(curList);
        } else {
            newList = curList;
        }
        return newList;
    }


    /**
     * 定时刷新交易记录
     *
     * @param walletAddress
     * @param direction
     * @return
     */
    private Flowable<List<Transaction>> getTransactionListWithTime(String walletAddress, String direction) {
        return getTransactionList(walletAddress, DIRECTION_NEW, getBeginSequenceByDirection(direction))
                .flatMap(new Function<Response<ApiResponse<List<Transaction>>>, SingleSource<List<Transaction>>>() {
                    @Override
                    public SingleSource<List<Transaction>> apply(Response<ApiResponse<List<Transaction>>> apiResponseResponse) throws Exception {
                        if (apiResponseResponse.isSuccessful() && apiResponseResponse.body().getResult() == ApiErrorCode.SUCCESS) {
                            return Single.just(apiResponseResponse.body().getData());
                        } else {
                            return Single.error(new Throwable());
                        }
                    }
                })
                .toFlowable();
    }


    /**
     * 切换钱包，每次都是获取最新的数据
     *
     * @param walletAddress
     * @return
     */
    private Single<List<Transaction>> getTransactionList(String walletAddress) {

        return getTransactionListFromDB(walletAddress).flatMap(new Function<List<Transaction>, SingleSource<List<Transaction>>>() {
            @Override
            public SingleSource<List<Transaction>> apply(List<Transaction> transactionList) throws Exception {
                LogUtils.d("getTransactionListFromDB success" + transactionList);
                return getTransactionList(walletAddress, DIRECTION_NEW, getBeginSequenceByDirection(DIRECTION_NEW))
                        .flatMap(new Function<Response<ApiResponse<List<Transaction>>>, SingleSource<List<Transaction>>>() {
                            @Override
                            public SingleSource<List<Transaction>> apply(Response<ApiResponse<List<Transaction>>> apiResponseResponse) throws Exception {
                                LogUtils.d("getTransactionList success" + apiResponseResponse.toString());
                                if (apiResponseResponse.isSuccessful() && apiResponseResponse.body().getResult() == ApiErrorCode.SUCCESS) {
                                    transactionList.addAll(apiResponseResponse.body().getData());
                                }
                                return Single.just(transactionList);
                            }
                        })
                        .doOnError(new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                LogUtils.d(throwable.getMessage());
                            }
                        });
            }
        });
    }

    /**
     * 从数据中获取未完成的交易记录
     *
     * @param walletAddress
     * @return
     */
    private Single<List<Transaction>> getTransactionListFromDB(String walletAddress) {

        LogUtils.d("getTransactionListFromDB" + walletAddress);

        return Flowable.fromCallable(new Callable<List<TransactionEntity>>() {
            @Override
            public List<TransactionEntity> call() throws Exception {
                return TransactionDao.getTransactionList(walletAddress);
            }
        }).flatMap(new Function<List<TransactionEntity>, Publisher<TransactionEntity>>() {
            @Override
            public Publisher<TransactionEntity> apply(List<TransactionEntity> transactionEntities) throws Exception {
                return Flowable.fromIterable(transactionEntities);
            }
        }).map(new Function<TransactionEntity, Transaction>() {
            @Override
            public Transaction apply(TransactionEntity transactionEntity) throws Exception {
                return transactionEntity.toTransaction();
            }
        }).map(new Function<Transaction, Transaction>() {
            @Override
            public Transaction apply(Transaction transaction) throws Exception {
                LogUtils.e("发送交易完成开始轮询。。" + transaction.toString());
                if (transaction.getTxReceiptStatus() == TransactionStatus.PENDING) {
                    TransactionManager.getInstance().getTransactionByLoop(transaction);
                }
                return transaction;
            }
        })
                .toList()
                .onErrorReturnItem(new ArrayList<>());
    }

    private Single<Response<ApiResponse<List<Transaction>>>> getTransactionList(String walletAddress, String direction, long beginSequence) {
        return ServerUtils
                .getCommonApi()
                .getTransactionList(ApiRequestBody.newBuilder()
                        .put("walletAddrs", new String[]{walletAddress})
                        .put("beginSequence", beginSequence)
                        .put("listSize", 20)
                        .put("direction", direction)
                        .build());
    }

    private long getBeginSequenceByDirection(String direction) {
        List<Transaction> transactionList = mTransactionMap.get(mWalletAddress);
        //拉最新的
        if (transactionList == null || transactionList.isEmpty()) {
            return -1;
        }
        //拉比当前最新的还大的
        if (DIRECTION_NEW.equals(direction)) {
            return getValidBiggerSequence(transactionList);
        } else {
            return getValidSmallerSequence(transactionList);
        }
    }

    private long getValidBiggerSequence(List<Transaction> transactionList) {

        return Flowable
                .range(0, transactionList.size())
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence() != 0;
                    }
                })
                .map(new Function<Integer, Long>() {
                    @Override
                    public Long apply(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence();
                    }
                })
                .firstElement()
                .defaultIfEmpty(-1L)
                .onErrorReturnItem(-1L)
                .blockingGet();
    }

    private long getValidSmallerSequence(List<Transaction> transactionList) {

        return Flowable
                .range(0, transactionList.size())
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence() != 0;
                    }
                })
                .lastElement()
                .map(new Function<Integer, Long>() {
                    @Override
                    public Long apply(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence();
                    }
                })
                .blockingGet();
    }
}
