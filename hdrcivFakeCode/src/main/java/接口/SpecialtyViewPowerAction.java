package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.enums.DictType;
import com.goodwill.hdr.civ.web.entity.*;
import com.goodwill.hdr.civ.web.service.SpecialtyViewNewService;
import com.goodwill.hdr.civ.web.service.SpecialtyViewPowerService;
import com.goodwill.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author guozhenlei
 * @Description 类描述：
 * @modify 修改记录：
 */
public class SpecialtyViewPowerAction extends CIVAction {
    @Autowired
    private SpecialtyViewNewService specialtyViewNewService;

    @Autowired
    private SpecialtyViewPowerService specialtyViewPowerService;


    /**
     * 获取疾病列表--管理员
     * String userCode = SecurityUtils.getCurrentUserName(); //在线用户
     */
    public void getSicknessList() {
        String mainDiag = getParameter("keyWord");
        Page<SicknessEntity> page = specialtyViewNewService.getSicknessList(mainDiag, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 获取科室列表--管理员
     * String userCode = SecurityUtils.getCurrentUserName(); //在线用户
     */
    public void getDeptList() {
        String dept = getParameter("keyWord");
        Page<SpecialtyDeptEntity> page = specialtyViewNewService.getDeptList(dept, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 疾病列表删除疾病
     */
    public void delSickness() {
        String sicknessCode = getParameter("scikness_code");
        int status = specialtyViewPowerService.delSickness(sicknessCode);
        renderJson("{\"status\":" + status + "}");
    }

    /**
     * 科室列表删除科室配置
     */
    public void delDeptConfig() {
        String deptCode = getParameter("dept_code");
        int status = specialtyViewPowerService.delDeptConfig(deptCode);
        renderJson("{\"status\":" + status + "}");
    }

    /**
     * 从Hbase字典表里新增数据--诊断疾病
     */
    public void getSickness() {
        String keyWord = getParameter("keyWord");
        //是否只显示已配置模板的诊断
        String isDisposed = getParameter("isDisposed");
        if (StringUtils.isBlank(keyWord)) {
            keyWord = "";
        }
        //管理员配置模板诊断
        Page<SicknessEntity> pageDiag = specialtyViewNewService.getSicknessList("", 0, 0);
        //当前用户配置的
        List<Map<String, String>> list = specialtyViewPowerService.getSicknessList();
        //是否只显示已配置模板诊断
        if (StringUtils.isNotBlank(isDisposed) && "1".equals(isDisposed)) {
            Page<Map<String, String>> page = specialtyViewPowerService.removeNoConfigData(pageDiag, list);
            renderJson(JsonUtil.getJSONString(page));
        } else {
            //查询HBASE数据
            Page<Map<String, String>> page = specialtyViewPowerService.getDictDiagData(keyWord, pageNo, pageSize, DictType.DIAG);
            page = specialtyViewPowerService.executeDuplicates(page, pageDiag);
            if (!"admin".equals(SecurityUtils.getCurrentUserName())) {
                page = specialtyViewPowerService.executeDuplicatesList(page, list);//非管理员用户可能配置管理员未设置模板的诊断，需要去重
            }
            renderJson(JsonUtil.getJSONString(page));
        }
    }
    /**
     *从mysql查询科室以供管理员添加配置
     */
    public void  getSpecialtyDepts(){
        String keyWord = getParameter("keyWord");
        //是否只显示已配置模板的诊断
        String isDisposed = getParameter("isDisposed");
        //首先查询管理员已经配置过的科室
        Page<Map<String,String>> page = specialtyViewNewService.getDeptListToAdd(keyWord,pageNo,pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }
    /**
     * 保存诊断数据到mysql
     */
    public void addSickness() {
        String sicknessCode = getParameter("code");
        String sicknessName = getParameter("name");
        int res = 0;
        if (StringUtils.isNotBlank(sicknessCode) && StringUtils.isNotBlank(sicknessName)) {
            res = specialtyViewPowerService.addSickness(sicknessCode, sicknessName);
        }
        renderJson("{\"status\":" + res + "}");
    }

    /**
     * 添加科室配置到mysql
     */
    public void  addConfigDept(){
        String deptCode = getParameter("code");
        String deptName = getParameter("name");
        int res = 0;
        if (StringUtils.isNotBlank(deptCode) && StringUtils.isNotBlank(deptName)) {
            res = specialtyViewPowerService.addSecurityDept(deptCode, deptName);
        }
        renderJson("{\"status\":" + res + "}");
    }

    /**
     * 从Hbase字典表里新增数据--生命体征
     */
    public void getHealth() {
        String keyWord = getParameter("keyWord");
        String code = getParameter("code");
        String configCode = getParameter("configType");
        if (StringUtils.isBlank(keyWord)) {
            keyWord = "";
        }
        //读取配置的生命体征
        Page<Map<String, String>> page =  new Page<Map<String, String>>();
        if ("diagnose".equals(configCode)) {
            page = specialtyViewPowerService.getHealthData(code, keyWord, pageNo, pageSize);
        } else if ("dept".equals(configCode)) {
            page = specialtyViewPowerService.getDeptHealthData(code, keyWord, pageNo, pageSize);
        }
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 从Hbase字典表里新增数据--检验明细
     */
    public void getLabDetail() {
        String keyWord = getParameter("keyWord");
        String code = getParameter("code");
        if (StringUtils.isBlank(keyWord)) {
            keyWord = "";
        }
        String configCode = getParameter("configType");
        Page<Map<String, String>> page =  new Page<Map<String, String>>();
        if ("diagnose".equals(configCode)) {
            page = specialtyViewPowerService.getDictData(code, "LabDetail", keyWord, pageNo, pageSize, DictType.LABSUB);
        }else if ("dept".equals(configCode)) {
            page = specialtyViewPowerService.getDeptDictData(code, "LabDetail", keyWord, pageNo, pageSize, DictType.LABSUB);
        }
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 从Hbase字典表里新增数据--检查
     */
    public void getExam() {
        String keyWord = getParameter("keyWord");
        String code = getParameter("code");
        if (StringUtils.isBlank(keyWord)) {
            keyWord = "";
        }
        String configCode = getParameter("configType");
        Page<Map<String, String>> page =  new Page<Map<String, String>>();
        if ("diagnose".equals(configCode)) {
            page = specialtyViewPowerService.getDictData(code, "Exam", keyWord, pageNo, pageSize, DictType.EXAM);
        }else if ("dept".equals(configCode)) {
            page = specialtyViewPowerService.getDeptDictData(code, "Exam", keyWord, pageNo, pageSize, DictType.EXAM);
        }
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 从Hbase字典表里新增数据--药品
     */
    public void getDrag() {
        String keyWord = getParameter("keyWord");
        String code = getParameter("code");
        if (StringUtils.isBlank(keyWord)) {
            keyWord = "";
        }
        String configCode = getParameter("configType");
        Page<Map<String, String>> page =  new Page<Map<String, String>>();
        if ("diagnose".equals(configCode)) {
            page = specialtyViewPowerService.getDictData(code, "Drag", keyWord, pageNo, pageSize, DictType.DRUG);
        }else if ("dept".equals(configCode)) {
            page = specialtyViewPowerService.getDeptDictData(code, "Drag", keyWord, pageNo, pageSize, DictType.DRUG);
        }
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 从Hbase字典表里新增数据--护理及饮食
     */
    public void getNurseAndEat() {
        String keyWord = getParameter("keyWord");
        String code = getParameter("code");
        if (StringUtils.isBlank(keyWord)) {
            keyWord = "";
        }
        String configCode = getParameter("configType");
        Page<Map<String, String>> page =  new Page<Map<String, String>>();
        if ("diagnose".equals(configCode)) {
            page = specialtyViewPowerService.getDictData(code, "NurseAndEat", keyWord, pageNo, pageSize, DictType.NURSE);
        }else if ("dept".equals(configCode)) {
            page = specialtyViewPowerService.getDeptDictData(code, "NurseAndEat", keyWord, pageNo, pageSize, DictType.NURSE);
        }
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 从Hbase字典表里新增数据--检验
     */
    public void getLab() {
        String keyWord = getParameter("keyWord");
        String code = getParameter("code");
        if (StringUtils.isBlank(keyWord)) {
            keyWord = "";
        }
        String configCode = getParameter("configType");
        Page<Map<String, String>> page =  new Page<Map<String, String>>();
        if ("diagnose".equals(configCode)) {
            page =  specialtyViewPowerService.getDictData(code, "Lab", keyWord, pageNo, pageSize, DictType.LAB);
        }else if ("dept".equals(configCode)) {
            page =  specialtyViewPowerService.getDeptDictData(code, "Lab", keyWord, pageNo, pageSize, DictType.LAB);
        }
        renderJson(JsonUtil.getJSONString(page));
    }

    /**
     * 获取诊断下的大项
     */
    public void getItemList() {
        String sicknessCode = getParameter("main_diag");
        List<SpecialtyConfigEntity> list = specialtyViewPowerService.getItemList(sicknessCode);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 获取专科科室下的大项
     */
    public void getSpecityaltyDeptItemList(){
        String deptCode = getParameter("dept_code");
        List<SpecialtyDeptConfigEntity> list = specialtyViewPowerService.getDeptItemList(deptCode);
        renderJson(JsonUtil.getJSONString(list));
    }

    /**
     * 获取诊断明细数据
     */
    public void getItemDetail() {
        String sicknessCode = getParameter("main_diag");
        String type = getParameter("type_code");
        Page<SpecialtyIndicatorConfigEntity> page = specialtyViewPowerService.getDetailData(sicknessCode, type, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }
    /**
     * 获取科室明细数据
     */
    public void  getDeptItemDetail(){
        String code = getParameter("dept_code");
        String type = getParameter("type_code");
        Page<Map<String,Object>> page = specialtyViewPowerService.getDeptDetailData(code, type, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(page));
    }
    /**
     * 根据id删除诊断明细
     */
    public void delInditorConfig() {
        String id = getParameter("id");
        int num = specialtyViewPowerService.delInditorConfig(id);
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("status", num);
        renderJson(JsonUtil.getJSONString(map));
    }
    /**
     * 根据id删除科室明细
     */
    public void delDeptInditorConfig() {
        String id = getParameter("id");
        int num = specialtyViewPowerService.delDeptInditorConfig(id);
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("status", num);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 添加诊断明细
     */
    public void addInditorConfig() {
        String mainDiag = getParameter("sickness_code");
        String mainDiagName = getParameter("sickness_name");
        String itemCode = getParameter("item_code");
        String itemName = getParameter("item_name");
        String itemIndicatorCode = getParameter("itemIndicatorCode");
        String itemIndicatorName = getParameter("itemIndicatorName");
        Map<String, String> map = new HashMap<String, String>();
        map.put("mainDiag", mainDiag);
        map.put("mainDiagName", mainDiagName);
        map.put("itemCode", itemCode);
        map.put("itemName", itemName);
        map.put("itemClassCode", "");
        map.put("itemClassName", "");
        map.put("itemIndicatorCode", itemIndicatorCode);
        map.put("itemIndicatorName", itemIndicatorName);
        int num = specialtyViewPowerService.addInditorConfig(map);
        Map<String, Integer> mapRetrun = new HashMap<String, Integer>();
        mapRetrun.put("status", num);
        renderJson(JsonUtil.getJSONString(mapRetrun));
    }

    /**
     * 添加科室明细
     */
    public void addDeptInditorConfig() {
        String deptCode = getParameter("dept_code");
        String deptName = getParameter("dept_name");
        String itemCode = getParameter("item_code");
        String itemName = getParameter("item_name");
        String itemIndicatorCode = getParameter("itemIndicatorCode");
        String itemIndicatorName = getParameter("itemIndicatorName");
        Map<String, String> map = new HashMap<String, String>();
        map.put("deptCode", deptCode);
        map.put("deptName", deptName);
        map.put("itemCode", itemCode);
        map.put("itemName", itemName);
        map.put("itemClassCode", "");
        map.put("itemClassName", "");
        map.put("itemIndicatorCode", itemIndicatorCode);
        map.put("itemIndicatorName", itemIndicatorName);
        int num = specialtyViewPowerService.addDeptInditorConfig(map);
        Map<String, Integer> mapRetrun = new HashMap<String, Integer>();
        mapRetrun.put("status", num);
        renderJson(JsonUtil.getJSONString(mapRetrun));
    }

    /**
     * 获取医生自定义的疾病列表
     */
    public void getConfigSickness() {
        List<Map<String, String>> list = specialtyViewPowerService.getSicknessList();
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        page.setPageSize(pageSize);
        page.setPageNo(pageNo);
        page.setTotalCount(list.size());
        page.setResult(list);
        renderJson(JsonUtil.getJSONString(list));
    }


}
