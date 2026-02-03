package com.library.seat.modules.seat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
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
    private java.util.Map<String, String> slotStatuses;
    
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
}
