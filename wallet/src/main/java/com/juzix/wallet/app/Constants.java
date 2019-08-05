package com.juzix.wallet.app;

/**
 * @author matrixelement
 */
public class Constants {

    public interface Common {
        //交易状态轮询时间
        int TRANSACTION_STATUS_LOOP_TIME = 2000;
        //交易列表轮询时间
        int TRANSCTION_LIST_LOOP_TIME = 10000;

    }

    public interface Extra {
        String EXTRA_PRIVATE_KEY = "extra_private_key";
        String EXTRA_PASSWORD = "extra_password";
        String EXTRA_KEYSTORE = "extra_keystore";
        String EXTRA_WALLET = "extra_walletEntity";
        String EXTRA_ADDRESS = "extra_address";
        String EXTRA_TO_ADDRESS = "extra_to_address";
        String EXTRA_TRANSACTION = "extra_transaction";
        String EXTRA_HASH = "extra_hash";
        String EXTRA_MNEMONIC = "extra_mnemonic";
        String EXTRA_PIC = "extra_pic";
        String EXTRA_SCAN_QRCODE_DATA = "extra_scan_qrcode_data";
        String EXTRA_TYPE = "extra_type";
        String EXTRA_SHARED_OWNERS = "extra_shared_owners";
        String EXTRA_REQUIRED_SIGNATURES = "extra_required_signatures";
        String EXTRA_WALLET_NAME = "extra_wallet_name";
        String EXTRA_WALLET_INDEX = "extra_index";
        String EXTRA_WALLET_SUB_INDEX = "extra_sub_index";
        String EXTRA_CANDIDATE = "extra_candidate_entity";
        String EXTRA_CANDIDATE_DETAIL = "extra_candidate_detail_entity";
        String EXTRA_VOTE = "extra_vote_entity";
        String EXTRA_ID = "extra_id";
        String EXTRA_CANDIDATE_ID = "extra_candidate_id";
        String EXTRA_CANDIDATE_NAME = "extra_candidate_name";
        String EXTRA_CANDIDATE_DEPOSIT = "extra_candidate_deposit";
        String EXTRA_TICKET_PRICE = "extra_ticket_price";
        String EXTRA_VOTE_ACTION = "extra_vote_action";
        String EXTRA_WALLET_ADDRESS = "extra_wallet_address";
        String EXTRA_WALLET_ICON = "extra_wallet_icon";
        String EXTRA_NODE_ADDRESS = "extra_node_address";
        String EXTRA_NODE_ICON = "extra_node_icon";
        String EXTRA_NODE_NAME = "extra_node_name";
    }

    public interface Bundle {

        String BUNDLE_WALLET = "bundle_wallet";
        String BUNDLE_TRANSFER_AMOUNT = "bundle_transfer_amount";
        String BUNDLE_TO_ADDRESS = "bundle_to_address";
        String BUNDLE_FEE_AMOUNT = "bundle_fee_amount";
        String BUNDLE_UUID = "bundle_UUID";
        String BUDLE_PASSWORD = "bundle_password";
        String BUNDLE_SHARE_APPINFO_LIST = "bundle_share_appinfo_list";
        String BUNDLE_TYPE = "bundle_type";
        String BUNDLE_FEE = "bundle_fee";
        String BUNDLE_TEXT = "bundle_text";
        String BUNDLE_MAP = "bundle_map";
    }

    public interface Action {

        String ACTION_NONE = "action_done";
        String ACTION_GET_ADDRESS = "action_get_address";
        String ACTION_SWITCH_LANGUAGE = "action_switch_language";
        String ACTION_CREATE_WALLET = "action_create_wallet";
        String ACTION_IMPORT_WALLET = "action_import_wallet";
    }

    public interface Preference {
        String KEY_SERVICE_TERMS_FLAG = "serviceTermsFlag";
        String KEY_OPERATE_MENU_FLAG = "operateMenuFlag";
        String KEY_LANGUAGE = "language";
        String KEY_FACE_TOUCH_ID_FLAG = "faceTouchIdFlag";
        String KEY_FIRST_ENTER = "firstEnter";
        String KEY_SHOW_ASSETS_FLAG = "showAssetsFlag";
        String KEY_UPDATE_VERSION_TIME = "updateVersionTime";
        String KEY_CURRENT_NODE_ADDRESS = "currentNodeAddress";
        String KEY_VALIDATORS_RANK = "validatorsRank";
    }

    public interface Permission {

    }

    public interface RequestCode {

        int REQUEST_CODE_SELEECT_WALLET_ADDRESS = 1;
        int REQUEST_CODE_SELEECT_WALLET = 2;
        int REQUEST_CODE_EDIT_ADDRESS = 3;
        int REQUEST_CODE_ADD_ADDRESS = 4;
        int REQUEST_CODE_GET_ADDRESS = 5;
        int REQUEST_CODE_SCAN_QRCODE = 6;

    }

    public interface URL {
        String UPDATE_URL = "https://download.platon.network/aton-update.json";
        String URL_TEST_A = "https://test-amigo.platon.network/test";
        String URL_TEST_B = "https://test-beta.platon.network/test";
        String URL_HTTP_A = "https://aton.platon.network";
        String URL_HTTP_B = "https://aton.platon.network";
        String URL_HTTP_C = "http://192.168.9.190:10061";
    }

    public interface VoteConstants {
        String IS_VALID = "0";// 0 表示无效
        String REQUEST_DIRECTION = "old";//请求参数传递的方向
        int LIST_SIZE = 10; //请求列表大小
        int NEWEST_DATA = -1; //请求最新的数据
        String REFRESH_DIRECTION = "new";//最新数据的方法
    }

    public interface DBName {
        //数据库名称
        String PORTAL = "portal";
        //2019/5/15
        int VERSION = 106;
    }

    public interface DelegateRecordType {
        String All = "all";
        String REDEEM = "redeem";
        String DELEGATE = "delegate";
    }

    public interface ValidatorsType {
        String ALL_VALIDATORS = "all";
        String ACTIVE_VALIDATORS = "active";
        String CANDIDATE_VALIDATORS = "candidate";
        String VALIDATORS_RANK = "rangking";
        String VALIDATORS_YIELD = "PA";
        String VALIDATORS_NODEID = "Validators_nodeId";
    }

    public interface Magnitudes {
        double TRILLION = 1E12;
        double HUNDRED_BILLION = 1E11;
        double TEN_BILLION = 1E10;
        double BILLION = 1E9;
        double HUNDRED_MILLION = 1E8;
        double TEN_MILLION = 1E7;
        double MILLION = 1E6;
        double HUNDRED_THOUSAND = 1E5;
        double TEN_THOUSAND = 1E4;
        double THOUSAND = 1E3;
        double HUNDRED = 1E2;

    }


}