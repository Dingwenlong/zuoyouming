package com.library.seat.modules.occupancy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.seat.modules.occupancy.entity.SeatOccupancy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeatOccupancyMapper extends BaseMapper<SeatOccupancy> {

    @Select("SELECT * FROM sys_seat_occupancy WHERE reservation_id = #{reservationId} LIMIT 1")
    SeatOccupancy selectByReservationId(@Param("reservationId") Long reservationId);

    @Select("SELECT * FROM sys_seat_occupancy WHERE occupancy_status IN ('normal', 'warning') ORDER BY update_time DESC")
    List<SeatOccupancy> selectActiveMonitoringList();

    @Select("SELECT * FROM sys_seat_occupancy WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 10")
    List<SeatOccupancy> selectRecentByUserId(@Param("userId") Long userId);
}
