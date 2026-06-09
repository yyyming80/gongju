package com.toolbox.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayOrder extends BaseEntity {
    
    private Long id;
    private LocalDateTime createTime;
    private Integer deleted;
    private String orderNo;
    private Long userId;
    private String productType;
    private Long productId;
    private BigDecimal amount;
    private String payMethod;
    private Integer payStatus;
    private LocalDateTime payTime;
    private String transactionId;
    private String remark;
    
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
    public Integer getPayStatus() { return payStatus; }
    public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
