package com.juzix.wallet;



import android.app.Application;
import com.juzhen.framework.network.ApiResponse;
import com.juzix.wallet.config.AppSettings;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.entity.Node;
import com.juzix.wallet.rxjavatest.RxJavaTestSchedulerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * 测试基类
 * 其他测试类需要的时候直接继承这个类，不需要每次去添加配置了
 *
 */
//@RunWith(RobolectricTestRunner.class)
//@Config(sdk = 27,manifest = Config.NONE,constants = BuildConfig.class)
public class BaseTestCase {
//    @Rule
//    public MockitoRule mockitoRule = MockitoJUnit.rule();
//    @Rule
//    public RxJavaTestSchedulerRule rule = new RxJavaTestSchedulerRule();
//
//    @Mock
//    public NodeManager nodeManager;
//    @Mock
//    public Node node;
//
//    @Before
//    public void setup() throws Exception {
//        AppSettings appSettings = AppSettings.getInstance();
//        nodeManager = NodeManager.getInstance();
//        node = new Node.Builder().build();
//        nodeManager.setCurNode(node);
//        //输出日志
//        ShadowLog.stream = System.out;
//        appSettings.init(RuntimeEnvironment.application);
////        ApiResponse.init(RuntimeEnvironment.application);
//    }
//    public Application getApplication() {
//        return RuntimeEnvironment.application;
//    }
//
////    public String getString(int id) {
////        return RuntimeEnvironment.application.getString(id);
////    }
//
////    public String getPkgName() {
////        return RuntimeEnvironment.application.getPackageName();
////    }
//
////    public <T extends Activity> T startActivity(Class<T> activityClass) {
////        return Robolectric.setupActivity(activityClass);
////    }
////
////    public void startFragment(Fragment fragment) {
////        SupportFragmentTestUtil.startFragment(fragment);
////    }


}
