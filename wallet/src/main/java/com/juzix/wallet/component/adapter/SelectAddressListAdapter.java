package com.juzix.wallet.component.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.juzhen.framework.util.RUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.component.adapter.base.ViewHolder;
import com.juzix.wallet.entity.Address;
import com.juzix.wallet.utils.AddressFormatUtil;

import java.util.List;

/**
 * @author matrixelement
 */
public class SelectAddressListAdapter extends CommonAdapter<Address> {

    private String mSenderAddress;

    public SelectAddressListAdapter(int layoutId, List<Address> datas, String senderAddress) {
        super(layoutId, datas);
        this.mSenderAddress = senderAddress;
    }

    @Override
    protected void convert(Context context, ViewHolder viewHolder, Address item, int position) {
        if (item != null) {
            viewHolder.setText(R.id.tv_wallet_name, item.getName());
            viewHolder.setText(R.id.tv_wallet_address, AddressFormatUtil.formatAddress(item.getAddress()));
            viewHolder.setVisible(R.id.iv_wallet_selected, false);
            int avatar = RUtils.drawable(item.getAvatar());
            if (avatar != -1) {
                viewHolder.setImageResource(R.id.iv_wallet_avatar, avatar);
            }
            viewHolder.getConvertView().setBackgroundColor(item.getPrefixAddress().equals(mSenderAddress) ? ContextCompat.getColor(context, R.color.color_33dcdfe8) : ContextCompat.getColor(context, R.color.color_ffffff));
        }
    }

    @Override
    public void updateItemView(Context context, int position, ViewHolder viewHolder) {
        if (mDatas != null && mDatas.size() > position) {
            convert(context, viewHolder, mDatas.get(position), position);
        }
    }
}
