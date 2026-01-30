package com.library.seat.modules.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_appeal")
@Schema(description = "违规申诉")
public class Appeal implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "关联预约记录ID")
    private Long reservationId;
    
    @Schema(description = "申诉理由")
    private String reason;
    
    @Schema(description = "图片凭证")
    private String images;
    
    @Schema(description = "状态: pending, approved, rejected")
    private String status;
    
    @Schema(description = "管理员回复")
    private String reply;
    
    @Schema(description = "创建时间")
    private Date createTime;
    
    @Schema(description = "更新时间")
    private Date updateTime;
    
    @Schema(description = "是否删除 0:否 1:是")
    private Integer deleted;
}
