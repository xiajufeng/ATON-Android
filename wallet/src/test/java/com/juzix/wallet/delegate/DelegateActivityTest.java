package com.juzix.wallet.delegate;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;
import com.juzhen.framework.network.ApiRequestBody;
import com.juzhen.framework.network.ApiResponse;
import com.juzhen.framework.network.ApiSingleObserver;
import com.juzhen.framework.util.NumberParserUtils;
import com.juzix.wallet.BuildConfig;
import com.juzix.wallet.component.ui.contract.DelegateContract;
import com.juzix.wallet.component.ui.presenter.DelegatePresenter;
import com.juzix.wallet.config.AppSettings;
import com.juzix.wallet.engine.DelegateManager;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.engine.ServerUtils;
import com.juzix.wallet.engine.Web3jManager;
import com.juzix.wallet.entity.AccountBalance;
import com.juzix.wallet.entity.DelegateDetail;
import com.juzix.wallet.entity.DelegateHandle;
import com.juzix.wallet.entity.Node;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.rxjavatest.RxJavaTestSchedulerRule;
import com.juzix.wallet.utils.RxUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.web3j.platon.StakingAmountType;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Convert;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import io.reactivex.functions.Consumer;
import rx.Subscriber;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE, constants = BuildConfig.class)
public class DelegateActivityTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public RxJavaTestSchedulerRule rule = new RxJavaTestSchedulerRule();

    @Mock
    public NodeManager nodeManager;
    @Mock
    public Node node;

    @Before
    public void setup() {
        Application app = RuntimeEnvironment.application;
        ApiResponse.init(app);

        AppSettings appSettings = AppSettings.getInstance();
        nodeManager = NodeManager.getInstance();
        node = new Node.Builder().build();
        nodeManager.setCurNode(node);

        //输出日志
        ShadowLog.stream = System.out;

        appSettings.init(app);

    }

    @Test
    public void testIsDelegate() {
        String walletAddress = "0xa577c0230df2cb329415bfebcb936496ab8ae2e4";
        String nodeId = "0x411a6c3640b6cd13799e7d4ed286c95104e3a31fbb05d7ae0004463db648f26e93f7f5848ee9795fb4bbb5f83985afd63f750dc4cf48f53b0e84d26d6834c20c";
        ServerUtils.getCommonApi().getIsDelegateInfo(ApiRequestBody.newBuilder()
                .put("addr", walletAddress)
                .put("nodeId", nodeId)
                .build())
                .subscribe(new ApiSingleObserver<DelegateHandle>() {
                    @Override
                    public void onApiSuccess(DelegateHandle delegateHandle) {
//                        presenter.getView().showIsCanDelegate(delegateHandle);
                        Log.d("result", "========================" + delegateHandle.isCanDelegation() + "====================" + delegateHandle.getMessage());
                    }

                    @Override
                    public void onApiFailure(ApiResponse response) {

                    }
                });
    }

    @Test
    public void testGetWalletBalance() {
        String[] walletAddressList = {"0x2e95e3ce0a54951eb9a99152a6d5827872dfb4fd", "0x4ded81199608adb765fb2fe029bbfdf57f538be8", "0x7e4f77a7daaba0c90851d388df02783511c2befa"};
        ServerUtils.getCommonApi().getAccountBalance(ApiRequestBody.newBuilder()
                .put("addrs", walletAddressList)
                .build())
                .compose(RxUtils.getSingleSchedulerTransformer())
                .subscribe(new ApiSingleObserver<List<AccountBalance>>() {
                    @Override
                    public void onApiSuccess(List<AccountBalance> accountBalances) {
                        Log.d("result", "message" + "--------------------" + accountBalances.toString());
                    }

                    @Override
                    public void onApiFailure(ApiResponse response) {

                    }
                });
    }

    @Test
    public void testGetGas() {
        Web3jManager.getInstance().getGasPrice()
                .compose(RxUtils.getSingleSchedulerTransformer())
                .subscribe(new Consumer<BigInteger>() {
                    @Override
                    public void accept(BigInteger bigInteger) throws Exception {
                        assertNotNull(bigInteger);
                        System.out.println(bigInteger);
                    }
                });
    }

    @Test
    public void testGetGasPrice() {
        String nodeId = "0x9e0b0968fd8977e41d321bc1f9d653f169368581028fd08c9419ddf28c1be1286aae51539aa416c2041261ae8aca4f3e2cd010dbbb1c79e14fdf432b38b8234a";
        Web3j web3j = Web3jManager.getInstance().getWeb3j();
        org.web3j.platon.contracts.DelegateContract delegateContract = org.web3j.platon.contracts.DelegateContract.load(web3j);
        StakingAmountType stakingAmountType = TextUtils.equals("balance", "balance") ? StakingAmountType.FREE_AMOUNT_TYPE : StakingAmountType.RESTRICTING_AMOUNT_TYPE;
        delegateContract.getDelegateFeeAmount(new BigInteger("500000"), nodeId, stakingAmountType, Convert.toVon("1000", Convert.Unit.LAT).toBigInteger())
                .subscribe(new Subscriber<BigInteger>() {
                    @Override
                    public void onNext(BigInteger integer) {
//                        view.showFeeAmount(integer.toString());
                        System.out.println(integer);
                        assertNotNull(integer);
                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                });
    }

    @Test
    public void testSortByFreeAccount() {
        List<Wallet> list = new ArrayList<>();

        Wallet wallet = new Wallet();
        wallet.setCreateTime(1115448481);
        wallet.setName("001");
        AccountBalance balance = new AccountBalance();
        balance.setFree("10000000084489");
        balance.setLock("0");
        wallet.setAccountBalance(balance);
        list.add(wallet);

        Wallet wallet2 = new Wallet();
        wallet2.setCreateTime(1115448485);
        wallet2.setName("002");
        AccountBalance balance2 = new AccountBalance();
        balance2.setFree("1000000005655655");
        balance2.setLock("0");
        wallet2.setAccountBalance(balance2);

        list.add(wallet2);

        Wallet wallet3 = new Wallet();
        wallet3.setName("003");
        wallet3.setCreateTime(1115448483);
        AccountBalance balance3 = new AccountBalance();
        balance3.setFree("10000000056556584855");
        balance3.setLock("0");
        wallet3.setAccountBalance(balance3);

        list.add(wallet3);


        Wallet wallet4 = new Wallet();
        wallet4.setName("004");
        wallet4.setCreateTime(1115448486);
        AccountBalance balance4 = new AccountBalance();
        balance4.setFree("1000000001156584855");
        balance4.setLock("0");
        wallet4.setAccountBalance(balance4);

        list.add(wallet4);


        Collections.sort(list, new Comparator<Wallet>() {
            @Override
            public int compare(Wallet o1, Wallet o2) {
               return Double.compare(NumberParserUtils.parseDouble(o2.getFreeBalance()), NumberParserUtils.parseDouble(o1.getFreeBalance()));
            }
        });


        Log.d("DelegateActivityTest", "=============" + list.size());

        for(Wallet bean :list){
            Log.d("DelegateActivityTest", "=============" + bean);
        }

    }

    @Test
    public  void testSortByCreateTime(){
        List<Wallet> list = new ArrayList<>();

        Wallet wallet = new Wallet();
        wallet.setCreateTime(1115448484);
        wallet.setName("001");
        AccountBalance balance = new AccountBalance();
        balance.setFree("10000000084489");
        balance.setLock("0");
        wallet.setAccountBalance(balance);
        list.add(wallet);

        Wallet wallet2 = new Wallet();
        wallet2.setCreateTime(1115448485);
        wallet2.setName("002");
        AccountBalance balance2 = new AccountBalance();
        balance2.setFree("1000000005655655");
        balance2.setLock("0");
        wallet2.setAccountBalance(balance2);

        list.add(wallet2);

        Wallet wallet3 = new Wallet();
        wallet3.setName("003");
        wallet3.setCreateTime(1115448483);
        AccountBalance balance3 = new AccountBalance();
        balance3.setFree("10000000056556584855");
        balance3.setLock("0");
        wallet3.setAccountBalance(balance3);

        list.add(wallet3);


        Wallet wallet4 = new Wallet();
        wallet4.setName("004");
        wallet4.setCreateTime(1115448486);
        AccountBalance balance4 = new AccountBalance();
        balance4.setFree("10000000054546556584855");
        balance4.setLock("0");
        wallet4.setAccountBalance(balance4);

        list.add(wallet4);


        Collections.sort(list, new Comparator<Wallet>() {
            @Override
            public int compare(Wallet o1, Wallet o2) {
                return Long.compare(o1.getCreateTime(), o2.getCreateTime());
            }
        });

        for(Wallet bean :list){
            Log.d("DelegateActivityTest", "=============" + bean);
        }

    }


}
