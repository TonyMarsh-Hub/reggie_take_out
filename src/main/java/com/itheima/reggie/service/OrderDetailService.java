package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.OrderDetail;

import java.util.List;

public interface OrderDetailService extends IService<OrderDetail> {

    List<OrderDetail> getDataByOrderIDAsList(Long orderId);

}
