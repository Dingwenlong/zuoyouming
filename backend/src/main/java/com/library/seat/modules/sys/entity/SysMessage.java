package com.library.seat.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_message")
@Schema(description = "消息广场发言")
public class SysMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String content;
    
    private Long atUserId;
    
    private Date createTime;
    
    private Integer deleted;

    // 辅助字段
    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String realName;

    @TableField(exist = false)
    private String role;

    @TableField(exist = false)
    private String avatar;

    @TableField(exist = false)
    private String status;

    @TableField(exist = false)
    private String seatNo;
}
