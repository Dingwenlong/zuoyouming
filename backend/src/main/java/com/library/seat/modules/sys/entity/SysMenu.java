package com.library.seat.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
@TableName("sys_menu")
@Schema(description = "系统菜单")
public class SysMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "菜单标题")
    private String title;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "权限角色JSON", example = "[\"admin\", \"student\"]")
    private String roles;

    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "子菜单")
    @TableField(exist = false)
    private List<SysMenu> children;
}
