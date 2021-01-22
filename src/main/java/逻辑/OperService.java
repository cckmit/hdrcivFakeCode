package 逻辑;

import com.goodwill.core.orm.MatchType;
import com.goodwill.core.orm.Page;
import com.goodwill.core.orm.Page.Sort;
import com.goodwill.core.orm.PropertyFilter;
import com.goodwill.core.utils.DateUtils;
import com.goodwill.hdr.civ.base.dao.HbaseDao;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.enums.HdrTableEnum;
import com.goodwill.hdr.civ.utils.CivUtils;
import com.goodwill.hdr.civ.utils.ColumnUtil;
import com.goodwill.hdr.civ.utils.DateUtil;
import com.goodwill.hdr.civ.utils.Utils;
import com.goodwill.hdr.civ.web.entity.CommonConfig;
import com.goodwill.security.utils.EncodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Decoder;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Service
public class OperService implements OperService {

    @Autowired
    private HbaseDao hbaseDao;

    @Override
    public Page<Map<String, String>> getOperList(String patientId, String visitType, String visitId, String orderBy,
                                                 String orderDir, int pageNo, int pageSize) {
        List<Map<String, String>> opers = new ArrayList<Map<String, String>>();
        //分页
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        boolean pageable = true;
        if (pageNo == 0 || pageSize == 0) {
            pageable = false;
        } else {
            page.setPageNo(pageNo);
            page.setPageSize(pageSize);
        }
        //排序
        if (StringUtils.isBlank(orderBy) || StringUtils.isBlank(orderDir)) {
            page.setOrderBy("OPER_START_TIME");
            page.setOrderDir("desc");
        } else {
            page.setOrderBy(orderBy);
            page.setOrderDir(orderDir);
        }
        //忽略门诊手术，仅查询住院手术
        if ("OUTPV".equals(visitType)) {
            return page;
        }
        //优先 根据vid查询
        List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
        filters1.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        //分页判断
        if (pageable) {
            page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, page,
                    filters1, "OPER_START_TIME", "OPERATION_CODE", "OPERATION_NAME", "OPER_NO", "ORDER_NO","INP_NO");
        } else {
            opers = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, filters1,
                    "OPER_START_TIME", "OPERATION_CODE", "OPERATION_NAME", "OPER_NO", "ORDER_NO","INP_NO");
            Utils.sortListByDate(opers, page.getOrderBy(), page.getOrderDir()); //排序
            page.setResult(opers);
            page.setTotalCount(opers.size());
        }

        //根据vid未找到，再根据入院出院时间查询
        if (page.getTotalCount() <= 0) {
            page = CivUtils.getInpOperas(page, patientId, visitId, pageable);
        }

        //上述均未找到，终止执行
        if (page.getTotalCount() <= 0) {
            return page;
        }
        //字段映射
        List<Map<String, String>> operas = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> opera = new HashMap<String, String>();
            ColumnUtil.convertMapping(opera, map, new String[]{"OPER_START_TIME", "OPERATION_CODE", "OPERATION_NAME",
                    "OPER_NO", "ORDER_NO","INP_NO"});
            operas.add(opera);
        }
        //获取外部配置手术url
        for (Map<String, String> map : operas) {
            if (StringUtils.isNotBlank(CommonConfig.getURL("OP"))) {
                map.put("linkType", CommonConfig.getLinkType("OP"));
            } else {
                map.put("linkType", "civ");
            }
        }
        //重置分页
        page.setResult(operas);
        return page;
    }

    @Override
    public Map<String, Object> getOperVisit(String patientId, String orderNo, String operNo) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        //优先 根据医嘱号查询
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
            filters1.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_VISIT.getCode(), patientId, filters1);
        }
        //根据医嘱号未查到，再根据手术序号查询  因为有的医院没有医嘱号
        if (list.size() == 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            filters2.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_VISIT.getCode(), patientId, filters2);
        }
        if (list.size() == 0) {
            result.put("status", "0");
            result.put("message", "未找到访视数据！");
            return result;
        }
        result.put("status", "1");
        Map<String, String> map = list.get(0);
        Map<String, String> info = new HashMap<String, String>();
//        Date date = DateUtils.convertStringToDate("yyyy.MM.dd", map.get("ANESTHETIST_DATE"));
        Date date = DateUtil.convertStringToDate( map.get("ANESTHETIST_DATE"));
        //手术开始时间  这里用麻醉签字时间代替
        Utils.checkAndPutToMap(info, "anesthetistDate", DateUtils.getDate("yyyy-MM-dd", date), "-", false);
        //电解质
        StringBuffer sb1 = new StringBuffer();
        String[] elements = new String[]{"K", "NA", "CL", "MG", "CA"};
        for (String element : elements) {
            String value = map.get("PRE_OPER_ELEC_" + element);
            if (StringUtils.isNotBlank(value)) {
                sb1.append(element + ":" + value + ",");
            } else {
                sb1.append(element + ":/,");
            }
        }
        String electric = sb1.substring(0, sb1.lastIndexOf(",")).toString() + ";";
        info.put("electric", electric);
        //术前八项
        StringBuffer sb2 = new StringBuffer();
        String[] items = new String[]{"HBSAG", "HBSAB", "HBEAB", "HBEAG", "HBCAB", "HCV", "HIV", "LUESRPR"};
        for (String item : items) {
            String value = map.get("PRE_OPER_" + item);
            if (StringUtils.isNotBlank(value)) {
                sb2.append(item + ":" + value + ",");
            } else {
                sb2.append(item + ":/,");
            }
        }
        String eight = sb2.substring(0, sb2.lastIndexOf(",")).toString() + ";";
        info.put("eight", eight);
        //字段映射
        ColumnUtil.convertMapping(info, map, new String[]{"OPERATION_NAME", "OPER_NO", "DIAG_BEFORE_OPERATION_NAME",
                "PRE_OPER_BP_VALUE", "PRE_OPER_HR_VALUE", "PRE_OPER_R_VALUE", "PRE_OPER_T_VALUE", "PRE_OPER_WT_VALUE",
                "PRE_OPER_BLOOD", "PRE_OPER_URINE", "PRE_OPER_KIDNEY", "PRE_OPER_SOLID", "PRE_OPER_LIVER",
                "PRE_OPER_OTHERS_LAB", "PRE_OPER_ALLERGY", "PRE_OPER_HEART", "PRE_OPER_LUNGS", "PRE_OPER_ECG",
                "PRE_OPER_OTHER_EXAM", "PRE_OPER_ASSESS"});
        result.put("info", info);
        return result;
    }

    @Override
    public Map<String, Object> getOperAnaes(String patientId, String orderNo, String operNo) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        //优先  根据医嘱号查询
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
            filters1.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, filters1);
        }
        //根据医嘱号未查询到  再根据手术编号查询
        if (list.size() == 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            filters2.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, filters2);
        }
        if (list.size() == 0) {
            result.put("status", "0");
            result.put("message", "未找到麻醉数据！");
            return result;
        }
        result.put("status", "1");
        Map<String, String> map = list.get(0);
        Map<String, String> info = new HashMap<String, String>();
        //计算麻醉时间
        String anesStartTime = map.get("ANESTHESIA_START_TIME");
        String anesEndTime = map.get("ANESTHESIA_END_TIME");
        if (StringUtils.isBlank(anesStartTime)) {
            info.put("anesTime", "开始时间未知");
        } else {
            if (StringUtils.isBlank(anesEndTime)) {
                info.put("anesTime", "结束时间未知");
            } else {
                //开始 结束时间均存在  计算麻醉时间
                long millis = CivUtils.calIntervalTime(anesStartTime, anesEndTime); //间隔毫秒
                if (millis != -1) {
                    info.put("anesTime", String.valueOf(millis / 60000) + " min");
                } else {
                    info.put("anesTime", "-");
                }
            }
        }
        //计算手术时间
        String operStartTime = map.get("OPER_START_TIME");
        String operEndTime = map.get("OPER_END_TIME");
        if (StringUtils.isBlank(operStartTime)) {
            info.put("operTime", "开始时间未知");
        } else {
            if (StringUtils.isBlank(operEndTime)) {
                info.put("operTime", "结束时间未知");
            } else {
                //开始 结束时间均存在  计算手术时间
                long millis = CivUtils.calIntervalTime(operStartTime, operEndTime); //间隔毫秒
                if (millis != -1) {
                    info.put("operTime", String.valueOf(millis / 60000) + " min");
                } else {
                    info.put("operTime", "-");
                }
            }
        }
        //字段映射
        ColumnUtil.convertMapping(info, map, new String[]{"ANESTHESIA_METHOD_NAME", "NARCOTIC_DRUG",
                "ASA_GRADE_CODE", "ANESTHESIA_DOCTOR_NAME", "ANESTHESIOLOGIST_ASSISTANT", "SURGEN_NAME",
                "ANESTHESIA_START_TIME"});
        result.put("info", info);
        return result;
    }

    @Override
    public Map<String, Object> getOperProcess(String patientId, String orderNo, String operNo) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, String>> anaesList = new ArrayList<Map<String, String>>();
        //先查询手术信息
        //查询手术过程
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
            filters1.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            anaesList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, filters1);
        }
        if (anaesList.size() == 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            filters2.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            anaesList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, filters2);
        }
        //查询术后访视
        List<Map<String, String>> operAfterList = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters3 = new ArrayList<PropertyFilter>();
            filters3.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            operAfterList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_AFTER.getCode(), patientId, filters3,
                    "OPER_TRSFU_AMOUNT_VALUE", "OPER_URINE_VOLUME_VALUE", "OPER_OTHER_NOTE", "OPER_DURATION",
                    "ORDER_NO");
        }
        if (operAfterList.size() == 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters4 = new ArrayList<PropertyFilter>();
            filters4.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            operAfterList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_AFTER.getCode(), patientId, filters4,
                    "OPER_TRSFU_AMOUNT_VALUE", "OPER_URINE_VOLUME_VALUE", "OPER_OTHER_NOTE", "OPER_DURATION",
                    "ORDER_NO");
        }
        //封装手术信息
        Map<String, String> info = new HashMap<String, String>();
        //获取手术过程信息
        Map<String, String> anaes = new HashMap<String, String>();
        if (anaesList.size() > 0) {
            anaes = anaesList.get(0);
        }
        //获取术后访视信息
        Map<String, String> after = new HashMap<String, String>();
        if (operAfterList.size() > 0) {
            after = operAfterList.get(0);
        }
        //若手术过程和术后访视均未找到
        if (anaesList.size() == 0 && operAfterList.size() == 0) {
            result.put("status", "0");
            result.put("message", "未找到手术信息！");
            return result;
        }
        result.put("status", "1");
        //处理手术时长
        String operDuration = after.get("OPER_DURATION");
        if (StringUtils.isNotBlank(operDuration)) {
            info.put("operDuration", operDuration + " min");
        } else {
            info.put("operDuration", "-");
        }
        //处理手术输血
        String blood = anaes.get("BLOOD_TRANSFUSION_AMOUNT_VALUE");
        if (StringUtils.isNotBlank(blood)) {
            info.put("bloodTransfusionAmountValue", blood + " mh");
        } else {
            info.put("bloodTransfusionAmountValue", "-");
        }
        //手术过程字段映射
        ColumnUtil.convertMapping(info, anaes, new String[]{"OPERATION_NAME", "OPER_NO", "OPERATION_GRADE_NAME",
                "OPER_ROOM_NAME", "OPER_ROOM_NO", "SURGEN_NAME", "FIRST_ASSISTANT_NAME", "SECOND_ASSISTANT_NAME",
                "INSTRUMENT_NURSE_NAME", "CIRCUIT_NURSE_NAME", "ANESTHESIA_DOCTOR_NAME", "OPER_START_TIME",
                "SURGICAL_POSITION_NAME", "SURGERY_SITE_NAME", "WOUND_LEVEL_NAME", "ANESTHESIA_METHOD_NAME",
                "DRAINAGE_SITE_NAME", "BLEEDING_AMOUNT_VALUE", "BLOOD_TRANSFUSION_TYPE_NAME", "OUT_OPER_DEPT_TIME",
                "OPER_STATUS_NAME"});
        //术后访视字段映射
        ColumnUtil.convertMapping(info, after, new String[]{"OPER_TRSFU_AMOUNT_VALUE", "OPER_URINE_VOLUME_VALUE",
                "OPER_OTHER_NOTE"});
        //存储手术信息
        result.put("info", info);
        return result;
    }

    @Override
    public Page<Map<String, String>> getOperDrug(String patientId, String orderNo, String operNo, int pageNo,
                                                 int pageSize) {
        //分页
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (pageSize == 0) {
            pageSize = 10;
        }
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        //查询手术用药
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
            filters1.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_OPER_PHARMACY.getCode(), patientId, page,
                    filters1);
        }
        if (page.getTotalCount() <= 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            filters2.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_OPER_PHARMACY.getCode(), patientId, page,
                    filters2);
        }
        //手术用药字段映射
        List<Map<String, String>> drugs = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> drug = new HashMap<String, String>();
            ColumnUtil.convertMapping(drug, map, new String[]{"DRUG_ID", "DOSAGE_UNIT", "AMOUNT_VALUE",
                    "PHARMACY_WAY_NAME"});
            drugs.add(drug);
        }
        //重置分页
        page.setResult(drugs);
        return page;
    }

    @Override
    public Map<String, Object> getOperRecovery(String patientId, String orderNo, String operNo) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        //查询恢复室
        List<Map<String, String>> recoveryList = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
            filters1.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            recoveryList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_RECOVERY.getCode(), patientId,
                    filters1);
        }
        if (recoveryList.size() == 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            filters2.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            recoveryList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_RECOVERY.getCode(), patientId,
                    filters2);
        }
        //查询手术镇痛
        List<Map<String, String>> analgesicList = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters3 = new ArrayList<PropertyFilter>();
            filters3.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            analgesicList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANALGESIC.getCode(), patientId,
                    filters3);
        }
        if (analgesicList.size() == 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters4 = new ArrayList<PropertyFilter>();
            filters4.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            analgesicList = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANALGESIC.getCode(), patientId,
                    filters4);
        }
        //封装恢复室信息
        Map<String, String> info = new HashMap<String, String>();
        Map<String, String> recovery = new HashMap<String, String>();
        if (recoveryList.size() > 0) {
            recovery = recoveryList.get(0);
        }
        //获取手术阵痛信息
        Map<String, String> analgesic = new HashMap<String, String>();
        if (analgesicList.size() > 0) {
            analgesic = analgesicList.get(0);
        }
        //恢复室信息和镇痛信息均未找到
        if (recoveryList.size() == 0 && analgesicList.size() == 0) {
            result.put("status", "0");
            result.put("message", "未找到恢复室数据！");
            return result;
        }
        result.put("status", "1");
        //恢复室字段映射
        ColumnUtil.convertMapping(info, recovery, new String[]{"ANESTHESIA_RECOVERY_DOCTOR_NAME",
                "IN_RECOVERY_ROOM_TIME", "OUT_RECOVERY_ROOM_TIME", "ANESTHESIA_RECOVERY_NURSE_NAME",
                "PACU_URINE_VOLUME_VALUE", "PACU_TRSFU_AMOUNT_VALUE", "PACU_DRAINAGE_VOLUME_VALUE", "RECOVERY_SCORE",
                "IN_OPER_WOUND_OBVM", "IN_OPER_STOMACH", "IN_OPER_PB_VALUE", "IN_OPER_CV_VALUE",
                "INFUSION_TRSFU_AMOUNT_VALUE", "IN_OPER_DISABILITY", "IN_OPER_POSITION", "OUT_OPER_O2_VALUE",
                "OUT_OPER_SP02", "OUT_RECOVERY_ROOM_TIME", "IN_OPER_HR_VALUE", "IN_OPER_R_VALUE", "IN_OPER_T_VALUE",
                "IN_OPER_BP_VALUE", "IN_OPER_VT_VALUE", "OUT_OPER_HR_VALUE", "OUT_OPER_R_VALUE", "OUT_OPER_T_VALUE",
                "OUT_OPER_BP_VALUE", "OUT_OPER_VT_VALUE"});
        //手术镇痛字段映射
        ColumnUtil.convertMapping(info, analgesic, new String[]{"PAIN_LOCATION_NAME", "PAIN_ACCESS_DEGREE",
                "PAIN_OCCUR_TIME", "ANALGESIC_TREATMENT_OPTIONS_NAME", "ANALGESIC_TREATMENT_TIME",
                "ANALGESIC_TREATMENT_RESULT_NAME", "ANALGESIC_DOCTOR_NAME",});
        //存储恢复室数据
        result.put("info", info);
        return result;
    }

    @Override
    public Map<String, Object> getOperAfter(String patientId, String orderNo, String operNo) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(orderNo)) {
            List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
            filters1.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
            list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_AFTER.getCode(), patientId, filters1);
        }
        if (list.size() == 0 && StringUtils.isNotBlank(operNo)) {
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            filters2.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), operNo));
            list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_AFTER.getCode(), patientId, filters2);
        }
        //未找到数据
        if (list.size() == 0) {
            result.put("status", "0");
            result.put("message", "未找到术后访视数据！");
            return result;
        }
        result.put("status", "1");

        //封装访视信息
        Map<String, String> info = new HashMap<String, String>();
        Map<String, String> map = list.get(0);
        //字段映射
        ColumnUtil.convertMapping(info, map, new String[]{"PATIENT_GO", "TIME_WAKE", "QT_SHXFHM", "ZT_AFTEROP",
                "EFFECT", "ANE_COMPLICATION", "QT_XTZT", "QT_SW", "EXTUBATION", "TIME_EXTUBATION", "CGQM_FYQ",
                "ZGNMA_ZDTT", "ZGNMA_MMYG", "ZGNMA_JWL", "ZGNMA_YMWXZ", "ZGNMA_YMWNZ", "ZGNMA_JT", "ZXJM_QX",
                "ZXJM_XX", "CGQM_SYSY", "ZXJM_JBXZ", "ZXJM_DG"});
        result.put("info", info);
        return result;
    }

    @Override
    public long getOperCount(String patientId, String visitId, String visitType) {
        //查询手术记录
        Page<Map<String, String>> page = getOperList(patientId, visitType, visitId, "", "", 0, 0);
        long operCount = page.getTotalCount();
        if (operCount < 0) {
            operCount = 0;
        }
        return operCount;
    }

    @Override
    public Page<Map<String, String>> getOpers(String patientId, String visitType, int pageNo, int pageSize, String orderby,
                                              String orderdir, String outPatientId) {
        //分页  排序
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        page.setPageNo(1);
        page.setPageSize(1000);
        if (StringUtils.isNotBlank(orderby) && StringUtils.isNotBlank(orderdir)) {
            page.setOrderBy(orderby);
            page.setOrderDir(orderdir);
        } else {
            page.setOrderBy("OPER_START_TIME");
            page.setOrderDir(Sort.DESC);
        }
        List<Map<String, String>> operas = new ArrayList<Map<String, String>>();

        if (StringUtils.isNotBlank(patientId)) {
            getOpersByPat(patientId, visitType, orderby, orderdir, operas);
        }
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] pats = outPatientId.split(",");
            for (int i = 0; i < pats.length; i++) {
                if (StringUtils.isNotBlank(pats[i])) {
                    String[] pat = pats[i].split("\\|");
                    getOpersByPat(pat[1], pat[0], orderby, orderdir, operas);
                }
            }
        }
        //添加是否配置了第三方url
        for (Map<String, String> opera : operas) {
            if (StringUtils.isNotBlank(CommonConfig.getURL("OP"))) {
                opera.put("linkType", CommonConfig.getLinkType("OP"));
            } else {
                opera.put("linkType", "html");
            }
        }
        //查询病案首页 手术记录
        //重置分页
        page.setResult(operas);
        return page;
    }

    /**
     * @Description 方法描述: 根据患者手术信息
     */
    public void getOpersByPat(String patientId, String visitType, String orderby, String orderdir, List<Map<String, String>> operas) {
        if ("01".equals(visitType)) {
            return;
        }
        Page<Map<String, String>> inpage = new Page<Map<String, String>>();
        inpage.setPageNo(1);
        inpage.setPageSize(1000);
        if (StringUtils.isNotBlank(orderby) && StringUtils.isNotBlank(orderdir)) {
            inpage.setOrderBy(orderby);
            inpage.setOrderDir(orderdir);
        } else {
            inpage.setOrderBy("OPER_START_TIME");
            inpage.setOrderDir(Sort.DESC);
        }
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();

        inpage = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, inpage, filters,
                "OPER_START_TIME", "OPERATION_CODE", "OPERATION_NAME", "OPER_NO", "ORDER_NO", "VISIT_ID","INP_NO");
        //字段映射
        if (inpage.getResult().size() > 0) {
            for (Map<String, String> map : inpage) {
                Map<String, String> opera = new HashMap<String, String>();
                ColumnUtil.convertMapping(opera, map, new String[]{"OPER_START_TIME", "OPERATION_CODE",
                        "OPERATION_NAME", "OPER_NO", "ORDER_NO", "VISIT_ID","INP_NO"});
                opera.put("patient_Id", patientId);
                operas.add(opera);
            }
        }
    }

    @Override
    public void getOpersNum(String patientId, String visitType, Map<String, Object> resultMap, String outPatientId) {
        // TODO Auto-generated method stub
        int num = 0;
        if (StringUtils.isNotBlank(patientId)) {
            num = num + getOpersByPat(patientId, visitType);
        }
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] pats = outPatientId.split(",");
            for (int i = 0; i < pats.length; i++) {
                if (StringUtils.isNotBlank(pats[i])) {
                    String[] pat = pats[i].split("\\|");
                    num = num + getOpersByPat(pat[1], pat[0]);
                }
            }
        }
        resultMap.put("num", num);
    }

    /**
     * @Description 方法描述: 根据患者手术数量
     */
    public int getOpersByPat(String patientId, String visitType) {
        if ("01".equals(visitType)) {
            return 0;
        }
        List<Map<String, String>> inpage = new ArrayList<Map<String, String>>();

        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        inpage = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patientId, filters,
                new String[]{"OPER_NO", "ORDER_NO"});
        return inpage.size();
    }

    /**
     * 手术配置
     *
     * @return
     */
    @Override
    public Map<String, String> getOperConfig() {
        // TODO Auto-generated method stub
        Map<String, String> rs = new HashMap<String, String>();
        String config = Config.getCIV_OPER_CONFIG();
        String[] conf = config.split(";");
        for (int i = 0; i < conf.length; i++) {
            String[] info = conf[i].split(",");
            rs.put(info[0], info[1]);
        }
        //是否配置手术外部url
        String operUrl = CommonConfig.getURL("OP");
        if (StringUtils.isNotBlank(operUrl)) {
            rs.put("isConfigUrl", "1");
        } else {
            rs.put("isConfigUrl", "0");
        }
        return rs;
    }

    /**
     * @return
     * @Description 方法描述:手术麻醉单显示配置
     */
    public List<Map<String, String>> getAnesthesiaConfig() {
    	List<Map<String, String>> list = new ArrayList<Map<String,String>>();
    	 Map<String, String> operConfig = new HashMap<String, String>();
    	 operConfig.put("code", "OperConfig");
    	 operConfig.put("name", "手术信息");
    	 list.add(operConfig);
        String config = Config.getCIV_ANESTHESIA_CONFIG();
        if(StringUtils.isNotBlank(config)) {
        	Map<String, String> rs = new HashMap<String, String>();
            rs.put("code","Anesthesia");
            rs.put("name",config);
            list.add(rs);
        }

        return list;
    }

    /**
     * @Description
     * 方法描述:手术麻醉单数据
     * @return 返回类型： Map<String,String>
     * @return
     */
    public List<Map<String, Object>> getAnesthesiaData(String patient_id, String order_no) {
        List<Map<String, Object>> configList = new ArrayList<Map<String, Object>>();
        if(StringUtils.isBlank(order_no)){
            return configList;
        }
        String config = Config.getCIV_ANESTHESIA_SELECT_CONFIG();
        //处理配置的具体内容
        String[] configes = config.split(";");
        Map<String, String> configMap = new HashMap<String, String>();
        for (String conf : configes) {
            String[] confes = conf.split("=");
            if (null != confes && confes.length > 1) {
                configMap.put(confes[0], confes[1]);
            }
        }
        try {
            configList = this.getOperAnesImg(patient_id,order_no,configMap);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return configList;
    }

    /**
     * 查询手术麻醉单
     * @param patient_id
     * @param oper_no
     * @param configMap
     * @return
     * @throws UnsupportedEncodingException
     */
    @Override
    public  List<Map<String, Object>> getOperAnesImg(String patient_id, String oper_no,Map<String, String> configMap)
            throws UnsupportedEncodingException {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        filters.add(new PropertyFilter("OPER_NO", "STRING", MatchType.EQ.getOperation(), oper_no));
        List<Map<String, String>> data =  hbaseDao.findConditionByPatient(HdrTableEnum.HDR_OPER_ANAES.getCode(), patient_id, filters,
                new String[]{"ANES_IMG_1", "ANES_IMG_2","ANES_IMG_3","ANES_IMG_4","ANES_IMG_5","ANES_IMG_6","ANES_IMG_7","ANES_IMG_8","ANES_IMG_9","ANES_IMG_10","ORDER_NO","IN_PATIENT_ID"});
        ServletContext servletContext = ServletActionContext.getServletContext();
        //这里返回给前端相对路径，定时清理通过绝对路径处理
        String path = servletContext.getRealPath("");
        String  relativePath = servletContext.getContextPath().replace("/", File.separator)+ File.separator + "smd_img";
        File file = new File(path + File.separator + "smd_img");
        file.mkdirs();
        path = file.getPath() + File.separator;
        for(Map<String, String> map: data){
            String rowkeyEncode = EncodeUtils.base64Encode(map.get("ROWKEY").getBytes());
            for (Map.Entry entry : map.entrySet()) {
                String key = (String)entry.getKey();
                String value = (String)entry.getValue();
                if (key.contains("ANES_IMG")) {
                    Map<String, Object> mapImg = new HashMap<String, Object>();
                    mapImg.put("name", StringUtils.isNotBlank(configMap.get(key))?configMap.get(key):key);
                    mapImg.put("code", key);
                    mapImg.put("path", relativePath);
                    Base64ToImage(value, rowkeyEncode + "_img="+key, path);
                    mapImg.put("img", rowkeyEncode + "_img="+key + ".jpg");
                    mapImg.put("type","image");
                    list.add(mapImg);
                }
            }
        }
        return list;
    }
    /**
     * base64字符串转换成图片
     * @param imgStr		base64字符串
     * @param imgFilePath	图片存放路径
     * @return
     * @author songhaibo
     * @dateTime 2019-12-17 14:42:17
     */
    public static boolean Base64ToImage(String imgStr, String rowkey, String imgFilePath) {
        // 对字节数组字符串进行Base64解码并生成图片
        if (StringUtils.isBlank(imgStr)) // 图像数据为空
            return false;

        BASE64Decoder decoder = new BASE64Decoder();
        try {
            // Base64解码
            byte[] b = decoder.decodeBuffer(imgStr);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {// 调整异常数据
                    b[i] += 256;
                }
            }
            String fileName = imgFilePath + rowkey + ".jpg";
            OutputStream out = new FileOutputStream(fileName);
            out.write(b);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}
