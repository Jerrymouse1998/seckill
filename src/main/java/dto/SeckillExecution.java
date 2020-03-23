package dto;

import enums.SeckillState;
import pojo.SuccessKilled;

//封装秒杀执行后的结果
public class SeckillExecution {
    private long seckillId;
    //秒杀状态标识
    private int state;
    //状态标识信息
    private String stateInfo;

    //秒杀成功信息
    private SuccessKilled successKilled;

    public SeckillExecution() {
    }

    public SeckillExecution(long seckillId, SeckillState seckillState, SuccessKilled successKilled) {
        this.seckillId = seckillId;
        this.state = seckillState.getState();
        this.stateInfo = seckillState.getStateInfo();
        this.successKilled = successKilled;
    }

    public SeckillExecution(long seckillId, SeckillState seckillState) {
        this.seckillId = seckillId;
        this.state = seckillState.getState();
        this.stateInfo = seckillState.getStateInfo();
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    public SuccessKilled getSuccessKilled() {
        return successKilled;
    }

    public void setSuccessKilled(SuccessKilled successKilled) {
        this.successKilled = successKilled;
    }

    @Override
    public String toString() {
        return "SeckillExecution{" +
                "seckillId=" + seckillId +
                ", state=" + state +
                ", stateInfo='" + stateInfo + '\'' +
                ", successKilled=" + successKilled +
                '}';
    }
}
