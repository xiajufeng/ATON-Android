package com.juzix.wallet.component.ui.contract;

import com.juzix.wallet.component.ui.base.IPresenter;
import com.juzix.wallet.component.ui.base.IView;
import com.juzix.wallet.entity.Address;

/**
 * @author matrixelement
 */
public class AddNewAddressContract {

    public interface View extends IView {

        Address getAddressFromIntent();

        String getName();

        String getAddress();

        void showNameError(String errContent);

        void showAddressError(String errContent);

        void setNameVisibility(int visibility);

        void setAddressVisibility(int visibility);

        void setAddressInfo(Address addressInfo);

        void setResult(Address addressEntity);

        void setBottonBtnText(String text);

        void showAddress(String address);
    }

    public interface Presenter extends IPresenter<View> {

        void loadAddressInfo();

        void addAddress();

        boolean checkAddressName(String name);

        boolean checkAddress(String address);

        void validQRCode(String text);
    }
}
