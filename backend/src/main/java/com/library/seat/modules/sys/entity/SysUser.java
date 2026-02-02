package com.library.seat.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "登录账号")
    private String username;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "密码")
    private String password;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "角色: student, librarian, admin")
    private String role;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "信用分", example = "100")
    private Integer creditScore;
    
    @Schema(description = "状态: active (正常), banned (封禁)", example = "active")
    private String status;
    
    @Schema(description = "最后登录时间")
    private Date lastLoginTime;

    @Schema(description = "创建时间")
    private Date createTime;
    
    @Schema(description = "更新时间")
    private Date updateTime;
    
    @Schema(description = "是否删除 0:否 1:是")
    private Integer deleted;

    @Schema(description = "微信OpenID")
    private String openId;
}
