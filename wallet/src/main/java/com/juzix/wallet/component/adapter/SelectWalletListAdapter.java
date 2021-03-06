package com.juzix.wallet.component.adapter;

import android.content.Context;
import android.widget.ListView;

import com.juzhen.framework.util.RUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.component.adapter.base.ViewHolder;
import com.juzix.wallet.component.ui.dialog.SelectWalletDialogFragment;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.AddressFormatUtil;
import com.juzix.wallet.utils.BigDecimalUtil;

import java.util.List;

/**
 * @author matrixelement
 */
public class SelectWalletListAdapter extends CommonAdapter<Wallet> {

    private ListView listView;
    private String action;

    public SelectWalletListAdapter(int layoutId, List<Wallet> datas, ListView listView, String action) {
        super(layoutId, datas);
        this.listView = listView;
        this.action = action;
    }

    @Override
    protected void convert(Context context, ViewHolder viewHolder, Wallet item, int position) {
        if (item != null) {
            viewHolder.getConvertView().setEnabled(isEnabled(item));
            viewHolder.setText(R.id.tv_wallet_name, item.getName());
//            viewHolder.setText(R.id.tv_wallet_balance, String.format("%s %s", "Balance", context.getString(R.string.amount_with_unit, NumberParserUtils.getPrettyNumber(item.getFreeBalance(), 8))));
            viewHolder.setText(R.id.tv_wallet_balance, AddressFormatUtil.formatAddress(item.getPrefixAddress()));
            viewHolder.setImageResource(R.id.iv_wallet_pic, RUtils.drawable(item.getAvatar()));
            viewHolder.setVisible(R.id.iv_selected, listView != null && listView.getCheckedItemPosition() == position);
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return isEnabled(getList().get(position));
    }

    private boolean isEnabled(Wallet item) {
        return !((SelectWalletDialogFragment.CREATE_SHARED_WALLET.equals(action) || SelectWalletDialogFragment.SELECT_TRANSACTION_WALLET.equals(action)) && BigDecimalUtil.isBigger(item.getFreeBalance(),"0"));
    }
}
