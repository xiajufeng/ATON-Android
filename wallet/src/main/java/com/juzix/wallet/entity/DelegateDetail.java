package com.juzix.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class DelegateDetail implements Parcelable {
    /**
     * 投票节点Id（节点地址 ）
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 机构官网（链接）
     */
    private String website;
    /**
     * 节点头像
     */
    private String url;
    /**
     * 竞选状态:
     * Active —— 活跃中
     * Candidate —— 候选中
     * Exiting —— 退出中
     * Exited —— 已退出
     */
    private String nodeStatus;
    /**
     * 已解除委托
     */
    private String released;

    private String walletAddress;

    /**
     * 是否为链初始化时内置的候选人
     * 0.7.3 新增字段
     */
    private boolean isInit;

    /**
     * 已委托  单位von
     */
    private String delegated;
    /**
     * 0.7.5新增字段
     */
    private boolean isConsensus;

    public DelegateDetail() {

    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNodeStatus() {
        return nodeStatus;
    }

    public void setNodeStatus(String nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public String getDelegated() {
        return delegated;
    }

    public void setDelegated(String delegated) {
        this.delegated = delegated;
    }

    public String getReleased() {
        return released;
    }

    public void setReleased(String released) {
        this.released = released;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public boolean isConsensus() {
        return isConsensus;
    }

    public void setConsensus(boolean consensus) {
        isConsensus = consensus;
    }

    protected DelegateDetail(Parcel in) {
        nodeId = in.readString();
        nodeName = in.readString();
        website = in.readString();
        nodeStatus = in.readString();
        url = in.readString();
        released = in.readString();
        walletAddress = in.readString();
        delegated = in.readString();
        isInit = in.readByte() != 0;
        isConsensus = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nodeId);
        dest.writeString(nodeName);
        dest.writeString(nodeStatus);
        dest.writeString(website);
        dest.writeString(url);
        dest.writeString(released);
        dest.writeString(walletAddress);
        dest.writeString(delegated);
        dest.writeByte((byte) (isInit ? 1 : 0));
        dest.writeByte((byte) (isConsensus ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DelegateDetail> CREATOR = new Creator<DelegateDetail>() {
        @Override
        public DelegateDetail createFromParcel(Parcel in) {
            return new DelegateDetail(in);
        }

        @Override
        public DelegateDetail[] newArray(int size) {
            return new DelegateDetail[size];
        }
    };
}
