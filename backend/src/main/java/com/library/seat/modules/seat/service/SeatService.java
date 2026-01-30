package com.library.seat.modules.seat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.common.Result;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.seat.mapper.SeatMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class SeatService extends ServiceImpl<SeatMapper, Seat> {

    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> addSeat(Seat seat) {
        // Unique check
        long count = this.count(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getSeatNo, seat.getSeatNo())
                .eq(Seat::getDeleted, 0));
        
        if (count > 0) {
            return Result.error("座位号已存在");
        }
        
        seat.setCreateTime(new Date());
        seat.setDeleted(0);
        if (seat.getStatus() == null) {
            seat.setStatus("available");
        }
        
        return Result.success(this.save(seat));
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateSeat(Seat seat) {
        Seat oldSeat = this.getById(seat.getId());
        if (oldSeat == null) {
            return Result.error("座位不存在");
        }
        
        // Unique check if seatNo changed
        if (seat.getSeatNo() != null && !seat.getSeatNo().equals(oldSeat.getSeatNo())) {
            long count = this.count(new LambdaQueryWrapper<Seat>()
                    .eq(Seat::getSeatNo, seat.getSeatNo())
                    .eq(Seat::getDeleted, 0));
            if (count > 0) {
                return Result.error("座位号已存在");
            }
        }
        
        seat.setUpdateTime(new Date());
        return Result.success(this.updateById(seat));
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteSeat(Long id) {
        Seat seat = this.getById(id);
        if (seat == null) {
            return Result.error("座位不存在");
        }
        
        if (!"available".equals(seat.getStatus()) && !"maintenance".equals(seat.getStatus())) {
            return Result.error("座位正在使用中，无法删除");
        }
        
        seat.setDeleted(1);
        seat.setUpdateTime(new Date());
        return Result.success(this.updateById(seat));
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchDelete(List<Long> ids) {
        for (Long id : ids) {
            Result<Boolean> res = deleteSeat(id);
            if (!res.isSuccess()) {
                throw new RuntimeException("删除失败: ID=" + id + ", " + res.getMsg());
            }
        }
        return Result.success(true);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchImport(List<Seat> seats) {
        for (Seat seat : seats) {
            Result<Boolean> res = addSeat(seat);
            if (!res.isSuccess()) {
                 // Or skip? For now, fail fast
                 throw new RuntimeException("导入失败: " + seat.getSeatNo() + ", " + res.getMsg());
            }
        }
        return Result.success(true);
    }
}
