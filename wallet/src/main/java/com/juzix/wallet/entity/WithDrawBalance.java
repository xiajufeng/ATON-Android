package com.juzix.wallet.entity;

import com.juzhen.framework.util.NumberParserUtils;
import com.juzix.wallet.utils.BigDecimalUtil;

public class WithDrawBalance {
//    /**
//     * 赎回中委托  单位von   1LAT(ETH)=1000000000000000000von(wei)
//     */
//    private String redeem;
    /**
     * 已锁定委托  单位von
     */
    private String locked;
    /**
     * 未锁定委托  单位von
     */
    private String unLocked;
    /**
     * 已解除委托  单位von
     */
    private String released;

    /**
     * 块高
     */
    private String stakingBlockNum;

    public WithDrawBalance() {

    }

    public String getLocked() {
        return locked;
    }

    public String getShowLocked() {
        return NumberParserUtils.getPrettyBalance(BigDecimalUtil.div(locked, "1E18"));
    }

    public void setLocked(String locked) {
        this.locked = locked;
    }

    public String getUnLocked() {
        return unLocked;
    }

    public String getShowUnLocked() {
        return NumberParserUtils.getPrettyBalance(BigDecimalUtil.div(unLocked, "1E18"));
    }

    public void setUnLocked(String unLocked) {
        this.unLocked = unLocked;
    }

    public String getReleased() {
        return released;
    }

    public String getShowReleased() {
        return NumberParserUtils.getPrettyBalance(BigDecimalUtil.div(released, "1E18"));
    }

    public void setReleased(String released) {
        this.released = released;
    }

    public String getStakingBlockNum() {
        return stakingBlockNum;
    }

    public void setStakingBlockNum(String stakingBlockNum) {
        this.stakingBlockNum = stakingBlockNum;
    }
}