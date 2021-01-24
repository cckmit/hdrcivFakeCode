package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.ConfigService;
import com.goodwill.hdr.civ.web.service.PowerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangsen
 * @Description 类描述：患者系统权限设置
 * @Date 2018年6月11日
 * @modify 修改记录：
 */
public class PowerAction extends CIVAction {

    private static final long serialVersionUID = 1L;

    @Autowired
    private PowerService powerService;

    @Autowired
    private ConfigService configService;



    //判断是否是管理员用户

    /**
     * @Description 判断是否是管理员用户
     */
    public void checkSysAdmin() {
        Map<String, String> rs = new HashMap<String, String>();
        String userCode = getParameter("userCode");
        rs = powerService.getCheckAdmin(userCode);
        renderJson(JsonUtil.getJSONString(rs));
    }

    //全局设置

    /**
     * @Description 获取全局设置
     */
    public void getSysConfig() {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        list = powerService.getSysConfig();
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * @Description 修改全局设置
     */
    public void updateSysConfig() {
        String configCode = getParameter("configCode");
        String configValue = getParameter("configValue");
        boolean b = powerService.updateSysConfig(configCode, configValue);
        Map<String, String> rs = new HashMap<String, String>();
        if (b) {
            rs.put("result", "1");
        } else {
            rs.put("result", "0");
        }
        renderJson(JsonUtil.getJSONString(rs));
    }

    //权限设置

    /**
     * @Description 获得分类视图权限
     */
    public void getPowerConfigByCategory() {
        String userCode = getParameter("userCode");
        List<Map<String, Object>> list = powerService.getPowerConfigByCategory(userCode);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * @Description 获得就诊视图权限
     */
    public void getPowerConfigByVisit() {
        String userCode = getParameter("userCode");
        List<Map<String, Object>> list = powerService.getPowerConfigByVisit(userCode);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * @Description 是否计算就诊视图Tab页签数据数量
     */
    public void getPowerConfigByTabNum() {
        Map<String, String> map = powerService.getSysConfigByType("CivVisit_CalcNum");
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 获得当前视图权限
     */
    public void getPowerConfigByPage() {
        String userCode = getParameter("userCode");
        Map<String, String> map = powerService.getPowerConfigByPage(userCode);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 获得Hide权限
     */
    public void getPowerConfigByHidePat() {
        Map<String, String> map = powerService.getSysConfigByType("StartUse_HidePatKeyM");
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 获得OrderClose权限
     */
    public void getPowerConfigByOrderClose() {
        Map<String, String> map = powerService.getSysConfigByType("StartUse_OrderClose");
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 获得Allergy权限
     */
    public void getPowerConfigByAllergy() {
        Map<String, String> map = powerService.getSysConfigByType("StartUse_Allergy");
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 获得用户所有权限
     */
    public void getPowerConfigByUser() {
        String userCode = getParameter("userCode");
        String deptCode = getParameter("deptCode");
        Map<String, Object> map = new HashMap<String, Object>();
        if (StringUtils.isNotBlank(deptCode)) {
            map = powerService.getPowerConfigByDept(deptCode);
        } else {
            map = powerService.getPowerConfigByUser(userCode);
        }
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 修改用户权限
     */
    public void updatePowerConfigByUser() {
        String userCode = getParameter("userCode");
        String deptCode = getParameter("deptCode");
        String Current = getParameter("Current");
        String Specialty = getParameter("Specialty");
        String TimeAxis = getParameter("TimeAxis");
        String Visit = getParameter("Visit");
        String Category = getParameter("Category");
        String Mr = getParameter("Mr");
        String Exam = getParameter("Exam");
        String Pathology = getParameter("Pathology");
        String specialtyTimeAxis = getParameter("SpecialtyTimeAxis");
        String medicalView = getParameter("MedicalView");
        Map<String, String> rs = new HashMap<String, String>();
        if (StringUtils.isNotBlank(deptCode)) {
            rs = powerService.updatePowerConfigByDept(deptCode, Current, Specialty, TimeAxis, Visit, Category, Mr, Exam,
                    Pathology, specialtyTimeAxis, medicalView);
        } else {
            rs = powerService.updatePowerConfigByUser(userCode, Current, Specialty, TimeAxis, Visit, Category, Mr, Exam,
                    Pathology, specialtyTimeAxis, medicalView);
        }
        renderJson(JsonUtil.getJSONString(rs));
    }

    /**
     * @Description 获得用户列表
     */
    public void getUserList() {
        String userName = getParameter("userName");
        String deptCode = getParameter("deptCode");
        String pageNo = getParameter("pageNo");
        String pageSize = getParameter("pageSize");
        Map<String, Object> map = powerService.getUserList(userName, deptCode, Integer.valueOf(pageNo),
                Integer.valueOf(pageSize));
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 获得医生列表
     */
    public void getDoctorList() {
        String userName = getParameter("userName");
        String deptCode = getParameter("deptCode");
        String pageNo = getParameter("pageNo");
        String pageSize = getParameter("pageSize");
        Map<String, Object> map = powerService.getUserList(userName, deptCode, Integer.valueOf(pageNo),
                Integer.valueOf(pageSize));
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 获得部门列表
     */
    public void getDeptList() {
        String deptName = getParameter("deptName");
        String pageNo = getParameter("pageNo");
        String pageSize = getParameter("pageSize");
        Page<Map<String, String>> map = powerService.getDeptList(deptName, Integer.valueOf(pageNo),
                Integer.valueOf(pageSize));
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * @Description 初始化所有科室权限
     */
    public void initDeptPower() {
        Map<String, String> map = powerService.initDeptPower();
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获取脱敏字段配置
     */
    public void getInfoHiddenScope() {
        List<Map<String, String>> list = powerService.getInfoHiddenField("");
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 更新选择状态
     */
    public void updateSysHideConfig() {
        String configCode = getParameter("configCode");
        String configValue = getParameter("configValue");
        String enabled = getParameter("enabled");
        boolean result = powerService.updateSysHideConfig(configCode, configValue, enabled);
        Map<String, String> map = new HashMap<String, String>();
        if (result) {
            map.put("status", "1");
        } else {
            map.put("status", "0");
        }
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获取通用配置
     */
    public void getCommonConfig() {
        //判断是否是管理员
        String userCode = getParameter("userCode");
        Map<String, Object> res = powerService.getCommonConfig(userCode);
        renderJson(JsonUtil.getJSONString(res));
    }

    /**
     * 获取患者列表的配置
     */
    public void getPatListConfig() {
        Map<String, Object> res = powerService.getPatListConfig();
        renderJson(JsonUtil.getJSONString(res));
    }

    /**
     * 获取当前视图的配置
     */
    public void getCurrentConfig() {
        Map<String, Object> res =  powerService.getCurrentConfig();
        renderJson(JsonUtil.getJSONString(res));
    }

    /**
     * 获取就诊试图配置
     */
    public void getVisitConfig() {
        String userCode = getParameter("userCode");
        Map<String, Object> res = powerService.getVisitConfig(userCode);
        renderJson(JsonUtil.getJSONString(res));
    }

    /**
     * 获取分类视图配置
     */
    public void getCatagoryConfig(){
        String userCode = getParameter("userCode");
        Map<String, Object> res = powerService.getCatagoryConfig(userCode,patientId,visitId);
        renderJson(JsonUtil.getJSONString(res));
    }

    /**
     * 获取oid配置列表
     */
    public void getOid(){


    }
}
