package com.juzix.wallet.component.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.juzhen.framework.util.RUtils;
import com.juzix.wallet.R;
import com.juzix.wallet.component.widget.CircleImageView;
import com.juzix.wallet.component.widget.ShadowDrawable;
import com.juzix.wallet.entity.DelegateInfo;
import com.juzix.wallet.utils.AddressFormatUtil;
import com.juzix.wallet.utils.BigDecimalUtil;
import com.juzix.wallet.utils.DensityUtil;
import com.juzix.wallet.utils.RxUtils;
import com.juzix.wallet.utils.StringUtil;



import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;

/**
 * 我的委托adapter
 */
public class MyDelegateAdapter extends RecyclerView.Adapter<MyDelegateAdapter.ViewHolder> {
    private Context mContext;
    private List<DelegateInfo> infoList;

    private OnItemClickListener mOnItemClickListener;

    public MyDelegateAdapter(List<DelegateInfo> infoList) {
        this.infoList = infoList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.mContext = parent.getContext();
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_my_delegate_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShadowDrawable.setShadowDrawable(holder.item,
                ContextCompat.getColor(mContext, R.color.color_ffffff),
                DensityUtil.dp2px(mContext, 4),
                ContextCompat.getColor(mContext, R.color.color_cc9ca7c2),
                DensityUtil.dp2px(mContext, 5),
                0,
                DensityUtil.dp2px(mContext, 2));
        DelegateInfo info = infoList.get(position);
        holder.walletIcon.setImageResource(RUtils.drawable(info.getWalletIcon()));
        holder.walletName.setText(info.getWalletName());
        holder.walletAddress.setText(AddressFormatUtil.formatAddress(info.getWalletAddress()));
        //转换的数据
        holder.available_balance.setText(TextUtils.equals(info.getAvailableDelegationBalance(),"0") ? "— —" :StringUtil.formatBalance(BigDecimalUtil.div(info.getAvailableDelegationBalance(), "1E18"))); // StringUtil.formatBalance(NumberParserUtils.parseDouble(NumberParserUtils.getPrettyBalance(BigDecimalUtil.div(info.getAvailableDelegationBalance(), "1E18"))), false)
        holder.totalDelegated.setText(TextUtils.equals(info.getDelegated(),"0") ? "— —" : StringUtil.formatBalance(BigDecimalUtil.div(info.getDelegated(), "1E18")));//StringUtil.formatBalance(NumberParserUtils.parseDouble(NumberParserUtils.getPrettyBalance(BigDecimalUtil.div(info.getDelegated(), "1E18"))), false)

        RxView.clicks(holder.itemView)
                .compose(RxUtils.getClickTransformer())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(info);
                        }
                    }
                });


    }

    public void notifyDataChanged(List<DelegateInfo> list) {
        this.infoList = list;
        notifyDataSetChanged();

    }

    public void notifyItemDataChanged(int positon, DelegateInfo info) {
        if (info == null) {
            return;
        }
        notifyItemChanged(positon, info);
    }

    @Override
    public int getItemCount() {
        if (infoList != null) {
            return infoList.size();
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_total_delegate)
        CircleImageView walletIcon;
        @BindView(R.id.tv_item_wallet_name)
        TextView walletName;
        @BindView(R.id.tv_item_wallet_address)
        TextView walletAddress;
        @BindView(R.id.tv_item_available_balance)
        TextView available_balance;
        @BindView(R.id.tv_item_delegate)
        TextView totalDelegated;
        @BindView(R.id.ll_item)
        LinearLayout item;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DelegateInfo delegateInfo);

    }
}
