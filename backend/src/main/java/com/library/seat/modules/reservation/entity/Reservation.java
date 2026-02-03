package com.library.seat.modules.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_reservation")
@Schema(description = "预约信息")
public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "预约ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "座位ID")
    private Long seatId;

    @TableField(exist = false)
    @Schema(description = "座位号")
    private String seatNo;
    
    @Schema(description = "开始时间")
    private Date startTime;
    
    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "截止时间 (签到/暂离)")
    private Date deadline;

    @Schema(description = "预约类型: appointment (预约), immediate (扫码入座)", example = "appointment")
    private String type;

    @Schema(description = "时段: morning(08:00-12:00), afternoon(13:00-17:00), evening(18:00-22:00)", example = "morning")
    private String slot;

    @TableField(exist = false)
    @Schema(description = "批量预约时段")
    private java.util.List<String> slots;
    
    @Schema(description = "状态: reserved, checked_in, completed, cancelled, violation", example = "reserved")
    private String status;
    
    @Schema(description = "创建时间")
    private Date createTime;
    
    @Schema(description = "更新时间")
    private Date updateTime;
    
    @Schema(description = "是否删除 0:否 1:是")
    private Integer deleted;
}
