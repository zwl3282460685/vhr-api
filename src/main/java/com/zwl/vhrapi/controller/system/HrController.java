package com.zwl.vhrapi.controller.system;

import com.zwl.vhrapi.model.Hr;
import com.zwl.vhrapi.model.RespBean;
import com.zwl.vhrapi.service.HrService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author zwl
 * @data 2020/12/9 20:33
 **/
@RestController
@RequestMapping("/system/hr")
@Api(tags = "操作员管理接口")
public class HrController {

    @Resource
    HrService hrService;

    @GetMapping("/")
    @ApiOperation("获取所有的hr")
    public List<Hr> getAllHrs(String keywords){
        return hrService.getAllHrs(keywords);
    }

    @PutMapping("/")
    @ApiOperation("修改hr的信息")
    public RespBean updateHr(@RequestBody Hr hr){
        if(hrService.updateHr(hr) == 1){
            return RespBean.ok("信息修改成功");
        }
        return RespBean.error("信息修改失败");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除hr")
    public RespBean deleteHrById(@PathVariable("id") Integer id) {
        if(hrService.deleteHrById(id) == 1){
            return RespBean.ok("删除成功");
        }
        return RespBean.error("删除失败");
    }
}
