package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private final DishFlavorService dishFlavorService;

    public DishServiceImpl(DishFlavorService dishFlavorService) {
        this.dishFlavorService = dishFlavorService;
    }


    /**
     * 新增菜品的同时，将口味数据也新增到对应数据表中
     *
     * @param dishDto 前端传递的包含口味信息的菜品数据
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品基本信息
        this.save(dishDto);
        // 保存菜品口味信息
        // 前端传递的口味数据中只有name和value，这里手动为每一个flavor绑定dishID
        Long dishID = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishID);
        }
        dishFlavorService.saveBatch(flavors);
    }

    @Transactional
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        // 修改菜品基本信息
        this.updateById(dishDto);
        // 前端传递的口味数据中只有name和value，这里手动为每一个flavor绑定dishID
        Long dishID = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishID);
        }
        // 先删除原有的口味数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishID);
        dishFlavorService.remove(queryWrapper);
        // 再新增新的口味数据
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public DishDto getDishDtoWithFlavorById(Long id) {
        Dish dish = this.getById(id);
        // 查询口味信息
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        // 将菜品基本信息和口味信息封装到dto中
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    /**
     * 修改菜品的销售状态：起售、停售
     *
     * @param status 销售状态
     * @param ids    菜品id集合
     */
    @Override
    public void updateStatus(Integer status, List<Long> ids) {
        //todo 需要完善的地方：当菜品在一个起售中的套餐中时，无法进行停售操作，提示用户需要先停售套餐
        Dish newStatus = new Dish();
        newStatus.setStatus(status);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);
        this.update(newStatus, queryWrapper);
    }
}
