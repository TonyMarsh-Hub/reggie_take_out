package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    private final CategoryService categoryService;
    private final SetmealService setmealService;
    private final DishService dishService;
    private final SetmealDishService setmealDishService;

    public SetmealController(CategoryService categoryService, SetmealService setmealService, DishService dishService, SetmealDishService setmealDishService) {
        this.categoryService = categoryService;
        this.setmealService = setmealService;
        this.dishService = dishService;
        this.setmealDishService = setmealDishService;
    }


    @CacheEvict(value = "setmealByCategory", allEntries = true)
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    @CacheEvict(value = "setmealByCategory", allEntries = true)
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        // 需要删除 1.套餐信息 2.套餐与菜品的关系信息
        setmealService.deleteWithDish(ids);
        return R.success("删除套餐成功");
    }

    @CacheEvict(value = "setmealByCategory", allEntries = true)
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐成功");
    }

    /**
     * 修改 停售、起售状态
     *
     * @param status 新的状态，1: 起售，0: 停售
     * @param ids    套餐id集合
     */
    @CacheEvict(value = "setmealByCategory", allEntries = true)
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        setmealService.updateStatus(status, ids);
        return R.success("修改成功");
    }

    /**
     * 根据id查询套餐信息，包裹嵌套的菜品信息等等，用于前端修改页面回显
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id) {
        Setmeal setmeal = setmealService.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        setmealDto.setSetmealDishes(setmealDishService.lambdaQuery().eq(SetmealDish::getSetmealId, id).list());
        setmealDto.setCategoryName(categoryService.getById(setmeal.getCategoryId()).getName());
        return R.success(setmealDto);
    }

    /**
     * 根据分类查询套餐
     */
    @Cacheable(value = "setmealByCategory", key = "#conditionWrapper.categoryId")
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal conditionWrapper) {
        List<Setmeal> setmealList = setmealService.getDataByCategoryIDAndStatusAsList(conditionWrapper);
        return R.success(setmealList);
    }

    @GetMapping("/page")
    public R<Page<SetmealDto>> page(int page, int pageSize, String name) {
        // 查询套餐的基础信息
        Page<Setmeal> setmealPage = setmealService.getDataByNameAsPage(page, pageSize, name);
        //将setmealPage转换为setmealDtoPage,添加分类名称与嵌套的菜品信息
        Page<SetmealDto> setmealDtoPage = new Page<>();
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");
        List<SetmealDto> setmealDtoRecords = setmealPage.getRecords().stream().map(setmeal -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);
            setmealDto.setCategoryName(setmeal.getCategoryId() == null ? "无数据" : categoryService.getById(setmeal.getCategoryId()).getName());
            // 发现在前端(分页列表中)不需要展示嵌套的菜品信息，所以这里可以不用查询相关信息，只需要查询分类名称即可  但是相关代码先暂时保留
            setmealDto.setSetmealDishes(setmealDishService.lambdaQuery().eq(SetmealDish::getSetmealId, setmeal.getId()).list());
            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(setmealDtoRecords);
        return R.success(setmealDtoPage);
    }

    /**
     * 查询一个套餐下的具体菜品信息
     *
     * @param setmeal 封装的查询条件
     */
    //TODO 用户端和管理端暂时还没有这个需求
    public R<List<Dish>> getDish(Setmeal setmeal) {
        // 1.查关系表，获取套餐绑定的菜品ID
        List<SetmealDish> setmealDishList = setmealDishService.getDataBySetmealIdAsList(setmeal.getId());
        List<Long> dishIds = setmealDishList.stream().map(SetmealDish::getDishId).collect(Collectors.toList());
        // 2.根据菜品ID查询菜品信息
        List<Dish> dishList = dishService.getDishListByMultiID(dishIds);
        return R.success(dishList);
    }

}
