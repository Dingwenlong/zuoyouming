package com.library.seat.modules.seat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@TableName("sys_seat")
@Schema(description = "座位信息")
public class Seat implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "座位ID")
    private Long id;

    @Schema(description = "座位号", example = "A-01")
    private String seatNo;

    @Schema(description = "区域", example = "A区")
    private String area;

    @Schema(description = "类型 (标准/靠窗/插座)", example = "标准")
    private String type;

    @Schema(description = "状态: available, occupied, maintenance", example = "available")
    private String status;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    @Schema(description = "各时段状态 (morning, afternoon, evening)")
    private Map<String, String> slotStatuses;

    @Schema(description = "X坐标")
    @JsonProperty("x")
    private Integer xCoord;

    @Schema(description = "Y坐标")
    @JsonProperty("y")
    private Integer yCoord;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "是否删除 0:否 1:是")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, String> getSlotStatuses() {
        return slotStatuses;
    }

    public void setSlotStatuses(Map<String, String> slotStatuses) {
        this.slotStatuses = slotStatuses;
    }

    public Integer getXCoord() {
        return xCoord;
    }

    public void setXCoord(Integer xCoord) {
        this.xCoord = xCoord;
    }

    public Integer getYCoord() {
        return yCoord;
    }

    public void setYCoord(Integer yCoord) {
        this.yCoord = yCoord;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
