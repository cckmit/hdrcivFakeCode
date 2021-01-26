package 逻辑;

import com.goodwill.core.orm.MatchType;
import com.goodwill.core.orm.Page.Sort;
import com.goodwill.core.orm.PropertyFilter;
import com.goodwill.core.orm.PropertyType;
import com.goodwill.core.utils.DateUtils;
import com.goodwill.hadoop.common.modal.ResultVO;
import com.goodwill.hadoop.solr.SolrQueryUtils;
import com.goodwill.hdr.civ.base.dao.HbaseDao;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.config.ConfigCache;
import com.goodwill.hdr.civ.enums.HdrConstantEnum;
import com.goodwill.hdr.civ.enums.HdrTableEnum;
import com.goodwill.hdr.civ.utils.*;
import com.goodwill.hdr.civ.web.dao.PowerDao;
import com.goodwill.hdr.civ.web.vidprocess.VidProcessConfig;
import com.goodwill.security.dao.DeptDao;
import com.goodwill.security.entity.Dept;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;

@Service
public class VisitService implements VisitService {

    Logger logger = LoggerFactory.getLogger(VisitServiceImpl.class);

    @Autowired
    private HbaseDao hbaseDao;
    @Autowired
    private DeptDao deptDao;
    @Autowired
    private VidProcessConfig vidProcess;
    @Autowired
    PowerDao powerDao;
    @Autowired
    private PowerService powerService;

    @Override
    public Map<String, String> getInfoFromPatient(String patientId, String visitType) {
        Map<String, String> infos = new HashMap<String, String>();
        infos.put("PATIENT_ID", patientId);
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if (StringUtils.isNotBlank(visitType)) {
            filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
        }
        //可能得到两条记录  门诊患者信息 和 住院患者信息
        List<Map<String, String>> indexs = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_PATIENT.getCode(),
                patientId, filters, new String[]{"EID", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "INP_NO",
                        "OUTP_NO", "IN_PATIENT_ID", "OUT_PATIENT_ID"});
        if (indexs.size() == 0) { //未找到患者
            infos.put("STATUS", "0");
            return infos;
        }
        //循环遍历患者信息  记录需要的字段
        for (Map<String, String> one : indexs) {
            Utils.checkAndPutToMap(infos, "PERSON_NAME", one.get("PERSON_NAME"), "", false);
            Utils.checkAndPutToMap(infos, "SEX_NAME", one.get("SEX_NAME"), "", false);
            Utils.checkAndPutToMap(infos, "EID", one.get("EID"), "", false);
            Utils.checkAndPutToMap(infos, "INP_NO", one.get("INP_NO"), "住院号未知", false);
            Utils.checkAndPutToMap(infos, "OUTP_NO", one.get("OUT_NO"), "门诊号未知", false);
            Utils.checkAndPutToMap(infos, "IN_PATIENT_ID", one.get("IN_PATIENT_ID"), "", false);
            Utils.checkAndPutToMap(infos, "OUT_PATIENT_ID", one.get("OUT_PATIENT_ID"), "", false);
            Utils.checkAndPutToMap(infos, "DATE_OF_BIRTH", one.get("DATE_OF_BIRTH"), "", false);
        }
        //处理出生日期
        String birthday = infos.get("DATE_OF_BIRTH");
        if (StringUtils.isNotBlank(birthday)) {
            infos.put("DATE_OF_BIRTH", Utils.getDate("yyyy-MM-dd", birthday));
        }
        return infos;
    }

    @Override
    public Map<String, Object> getOutOrInPatientId(String patientId, String visitType) {
        Map<String, Object> result = new HashMap<String, Object>();
        //就诊类型和患者标识去重
        Map<String, String> visitPidMap = new HashMap<String, String>();
        //存储关联的患者标识
        List<String> list = new ArrayList<String>();
        Map<String, String> patInfo = getInfoFromPatient(patientId, visitType);
        //先放入当前传入的患者标识 就诊类型
        if (StringUtils.isNotBlank(visitType) && StringUtils.isNotBlank(patientId)) {
            visitPidMap.put(visitType + "|" + patientId, visitType + "|" + patientId);
        }
        //获取主索引
        String eid = patInfo.get("EID");
        //检索solr，根据主索引关联所有的患者标识
        if (StringUtils.isNotBlank(eid)) {
            //查询条件
            String q = "*:* AND ";
            q += "EID:" + eid;
            //通过EID查询solr
            ResultVO docsMap = SolrQueryUtils.querySolr(SolrUtils.patInfoCollection, q, null, new String[]{"IN_PATIENT_ID", "OUT_PATIENT_ID", "INP_NO", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "VISIT_TYPE_CODE"}, 10000, 1, null, null);
            List<Map<String, String>> docList = docsMap.getResult();
            for (Map<String, String> doc : docList) {
                String pid = (String) (StringUtils.isBlank((String) doc.get("IN_PATIENT_ID"))
                        ? doc.get("OUT_PATIENT_ID") : doc.get("IN_PATIENT_ID"));
                String visit_type = (String) doc.get("VISIT_TYPE_CODE");
                if (StringUtils.isNotBlank(pid) && StringUtils.isNotBlank(visit_type)) {
                    visitPidMap.put(visit_type + "|" + pid, visit_type + "|" + pid);
                }
            }
        }
        //存储关联的患者标识
        boolean status = false;
        for (String key : visitPidMap.keySet()) {
            if (!(visitType + "|" + patientId).equals(key)) {
                status = true; //门诊标识和住院标识不一致，可能有多个
                //去除重复
                list.add(key);
            }
        }
        result.put("status", status);
        //转化为字符串
        String ids = StringUtils.join(list.toArray(), ",");
        result.put("ids", ids);
        return result;
    }

    /**
     * 将某个患者iD的诊断信息转化为map
     *
     * @param diags
     * @return
     */
    public Map<String, Object> transDiagInfoToMap(List<Map<String, String>> diags) {
        Map<String, Object> diagsMap = new HashMap<>();
        for (Map<String, String> map : diags) {
            diagsMap.put(map.get("VISIT_ID").trim(), map);
        }
        return diagsMap;
    }

    /**
     * @param visits    存储就诊列表的集合
     * @param patientId 患者编号
     * @param visitType 就诊类型
     * @return 返回类型： void
     * @Description 方法描述: 根据患者编号和就诊类型查询该类型下的就诊列表，并将数据填充到visits
     */
    private void getVisitsAndAddToVisits(List<Map<String, String>> visits, String patientId, String visitType,
                                         String dtype, String startDate, String endDate) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        Map<String, Object> emrDiagsMap = new HashMap<>();
        Map<String, Object> inpDiagsMap = new HashMap<>();
        //首先获取次patientid下的所有诊断，防止循环查询诊断数据
        if ("02".equals(visitType)) {
            List<Map<String, String>> emrDiags = this.getPatMainDiagInpbyConfig(patientId, "");//未出院  病历诊断
            List<Map<String, String>> inpDiags = this.getPatInpDiag(patientId, "");//出院  病案诊断
            //处理成map格式
            emrDiagsMap = transDiagInfoToMap(emrDiags);
            inpDiagsMap = transDiagInfoToMap(inpDiags);
        } else {
            List<Map<String, String>> outpDiags = this.getPatDiagOutp(patientId, "", "");
            inpDiagsMap = transDiagInfoToMap(outpDiags);
        }
        //住院
        if ("02".equals(visitType)) {
            //时间条件
            if ("1".equals(dtype)) { //仅一个时间
                filters.add(new PropertyFilter("ADMISSION_TIME", "STRING", MatchType.GE.getOperation(), startDate));
            } else if ("2".equals(dtype)) {
                filters.add(new PropertyFilter("ADMISSION_TIME", "STRING", MatchType.GE.getOperation(), startDate));
                filters.add(new PropertyFilter("ADMISSION_TIME", "STRING", MatchType.LE.getOperation(), endDate));
            }
            //			filters.add(new PropertyFilter("TRANS_NO", "STRING", MatchType.EQ.getOperation(), "0")); //仅取入出院，不取转科
            filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), "02"));
            List<Map<String, String>> patAdts = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_PAT_ADT.getCode(),
                    patientId, filters,
                    new String[]{"DEPT_DISCHARGE_FROM_CODE", "DEPT_DISCHARGE_FROM_NAME", "ADMISSION_TIME",
                            "DISCHARGE_TIME", "VISIT_ID", "DEPT_ADMISSION_TO_NAME", "DEPT_ADMISSION_TO_CODE", "INP_NO",
                            "IN_PATIENT_ID", "TRANS_NO", "CURR_DEPT_NAME", "CURR_DEPT_CODE", "DISTRICT_ADMISSION_TO_NAME"});
            Utils.sortListMulti(patAdts, new String[]{"TRANS_NO"}, new String[]{"asc"});
            //存在转科的只取最后一次转科信息
            patAdts = deleteMultiVisit(patAdts);
            for (Map<String, String> adt : patAdts) {
                Map<String, String> covert = new HashMap<String, String>();
                covert.put("END_DEPT_CODE", adt.get("DEPT_DISCHARGE_FROM_CODE")); //出院科室
                covert.put("END_DEPT_NAME", adt.get("DEPT_DISCHARGE_FROM_NAME"));
                if ("0".equals(adt.get("TRANS_NO"))) {
                    covert.put("START_DEPT_NAME", adt.get("DEPT_ADMISSION_TO_NAME")); //入院科室
                    covert.put("START_DEPT_CODE", adt.get("DEPT_ADMISSION_TO_CODE"));
                } else {
                    covert.put("START_DEPT_NAME", adt.get("CURR_DEPT_NAME")); //当前科室
                    covert.put("START_DEPT_CODE", adt.get("CURR_DEPT_CODE"));
                }
                covert.put("START_TIME", adt.get("ADMISSION_TIME")); //入院时间
                covert.put("END_TIME", adt.get("DISCHARGE_TIME")); //出院时间
                covert.put("VISIT_ID", adt.get("VISIT_ID").trim()); //就诊次数
                covert.put("DISTRICT_ADMISSION_TO_NAME", adt.get("DISTRICT_ADMISSION_TO_NAME")); //病区
                //查询本次住院诊断  主诊断
                String visitId = adt.get("VISIT_ID").trim();
                List<Map<String, String>> inpDiags = new ArrayList<Map<String, String>>();
                if (StringUtils.isBlank(adt.get("DISCHARGE_TIME"))) {
                    //TODO 由于现场主诊断读取方式不一样，注释掉原方法，临时用新方法代替
                    //				List<Map<String, String>> inpDiags = this.getPatDiagInp(patientId, visitId, "2,3", "1");
//					inpDiags = this.getPatMainDiagInpbyConfig(patientId, visitId);
                    if (!emrDiagsMap.isEmpty()) {
                        Map<String, String> mapTmp = (Map<String, String>) emrDiagsMap.get(visitId);
                        if(null != mapTmp) {
                            inpDiags.add(mapTmp);
                        }
                    }
                } else {
//					inpDiags = this.getPatInpDiag(patientId, visitId);
                    if (!inpDiagsMap.isEmpty()) {
                        Map<String, String> mapTmp = (Map<String, String>) inpDiagsMap.get(visitId);
                        if( null != mapTmp) {
                            inpDiags.add(mapTmp);
                        }
                    }
                }
                if (inpDiags.size() > 0) {
                    covert.put("DIAGNOSIS_CODE", inpDiags.get(0).get("DIAGNOSIS_CODE")); //诊断编码
                    covert.put("DIAGNOSIS_NAME", inpDiags.get(0).get("DIAGNOSIS_NAME")); //诊断名称
                } else {
                    covert.put("DIAGNOSIS_CODE", "");
                    covert.put("DIAGNOSIS_NAME", "");
                }
                if (!covert.isEmpty()) {
                    covert.put("INP_NO", adt.get("INP_NO"));
                    covert.put("VISIT_TYPE", "INPV");
                    covert.put("NOW_PATIENT", adt.get("IN_PATIENT_ID"));//区分传入的患者编号和通过EID关联出来的患者编号
                    visits.add(covert);
                }
            }
        } else {
            //时间条件
            if ("1".equals(dtype)) {
                filters.add(new PropertyFilter("VISIT_TIME", "STRING", MatchType.GE.getOperation(), startDate));
            } else if ("2".equals(dtype)) {
                filters.add(new PropertyFilter("VISIT_TIME", "STRING", MatchType.GE.getOperation(), startDate));
                filters.add(new PropertyFilter("VISIT_TIME", "STRING", MatchType.LE.getOperation(), endDate));
            }
            List<Map<String, String>> outVisits = hbaseDao
                    .findConditionByPatient(HdrTableEnum.HDR_OUT_VISIT.getCode(), patientId, filters,
                            new String[]{"REGISTING_TIME", "VISIT_TIME", "VISIT_ID", "VISIT_DEPT_NAME",
                                    "VISIT_DEPT_CODE", "VISIT_FLAG", "OUT_PATIENT_ID", "EMERGENCY_VISIT_IND",
                                    "VISIT_STATUS_NAME", "OUTP_NO"});
            for (Map<String, String> outVisit : outVisits) {
                //北医三院特殊处理：1-挂号未就诊 9-退号，忽略这两类门诊信息
                if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache("org_oid"))) {
                    String visit_flag = outVisit.get("VISIT_FLAG");
                    if (visit_flag != null && "1,9,".indexOf(visit_flag + ",") > -1) {
                        continue;
                    }
                }
                if (StringUtils.isBlank(outVisit.get("VISIT_DEPT_NAME")) || StringUtils.isBlank(outVisit.get("VISIT_TIME"))) {
                    continue;
                }
                Map<String, String> covert = new HashMap<String, String>();
                covert.put("START_DEPT_NAME", outVisit.get("VISIT_DEPT_NAME")); //就诊科室
                covert.put("START_DEPT_CODE", outVisit.get("VISIT_DEPT_CODE"));
                covert.put("START_TIME", outVisit.get("VISIT_TIME")); //就诊时间
                covert.put("VISIT_ID", outVisit.get("VISIT_ID").trim()); //就诊次数
                covert.put("VISIT_NO", outVisit.get("OUTP_NO")); //就诊号
                //查询本次门诊诊断
                String visitId = outVisit.get("VISIT_ID").trim();
//				List<Map<String, String>> outpDiags = this.getPatDiagOutp(patientId, visitId, "");
                List<Map<String, String>> outpDiags = new ArrayList<Map<String, String>>();
                if(!inpDiagsMap.isEmpty()) {
                    Map<String, String>  mapTmp =  (Map<String, String>) inpDiagsMap.get(visitId);
                    if(null != mapTmp) {
                        outpDiags.add(mapTmp);
                    }
                }
                if (outpDiags.size() > 0) {
                    covert.put("DIAGNOSIS_CODE", outpDiags.get(0).get("DIAGNOSIS_CODE"));
                    covert.put("DIAGNOSIS_NAME", outpDiags.get(0).get("DIAGNOSIS_NAME"));
                } else {
                    covert.put("DIAGNOSIS_CODE", "");
                    covert.put("DIAGNOSIS_NAME", "");
                }
                //若就诊时间没有，用挂号时间代替
                String registing_time = outVisit.get("REGISTING_TIME");
                if (StringUtils.isBlank(covert.get("START_TIME"))) {
                    covert.put("START_TIME", registing_time);
                }
                if (!covert.isEmpty()) {
                    //区分门诊和急诊
                    if ("true".equals(outVisit.get("EMERGENCY_VISIT_IND"))) {
                        covert.put("VISIT_TYPE", "EMPV");
                    } else {
                        covert.put("VISIT_TYPE", "OUTPV");
                    }
                    covert.put("NOW_PATIENT", outVisit.get("OUT_PATIENT_ID"));
                    covert.put("VISIT_STATUS_NAME", outVisit.get("VISIT_STATUS_NAME"));
                    visits.add(covert);
                }
            }
        }
    }

    /**
     * @param   患者基本信息
     * @param  患者编号
     * @return
     * @Description 方法描述: 通过EID或PATIENT_ID查询患者所有的就诊信息，优先EID
     */
    private List<Map<String, String>> getAllVisits(Map<String, String> pidInpNo,String dtype, String startDate, String endDate) {
        //就诊列表
        List<Map<String, String>> visits = new ArrayList<Map<String, String>>();
        //根据上述筛选后的患者编号和就诊类型   获取所有就诊列表
        for (Entry<String, String> entry : pidInpNo.entrySet()) {
            String[] vtpid = entry.getKey().split("\\|");
            getVisitsAndAddToVisits(visits, vtpid[1], vtpid[0], dtype, startDate, endDate);
        }
        return visits;
    }

    public Map<String, String> getAllPids(Map<String, String> patInfos, String patientId, String visitType,String deptType) {
        String visitPid = visitType + "|" + patientId;
        //key:就诊类型|门诊和住院患者编号    value:就诊类型
        Map<String, String> pidInpNo = new HashMap<String, String>();
        //先放入当前传入的就诊类型 和 患者编号
        if (StringUtils.isNotBlank(visitType) && StringUtils.isNotBlank(patientId) && (StringUtils.isBlank(deptType) || "all".equals(deptType) || visitType.equals(deptType))) {
            pidInpNo.put(visitPid, visitPid);
        }
        //根据主索引查询 Solr 处理门诊患者标识和住院患者标识不一致情况
        String EID = patInfos.get("EID");
        if (StringUtils.isNotBlank(EID) && !"EID未知".equals(EID)) {
            //查询条件
            String q = "EID:" + EID;
//            List<String> qparams = new ArrayList<String>();
//            qparams.add("EID:" + EID);
            if (!"all".equals(deptType) && StringUtils.isNotBlank(deptType)) {
//                qparams.add("VISIT_TYPE_CODE:" + deptType);
                q += " AND VISIT_TYPE_CODE:" + deptType;
            }
//            Map<String, String> params = new Hashtable<String, String>();
//            //返回结果
//            params.put("start", "0");
//            params.put("rows", "10000");
//            params.put("fl", "IN_PATIENT_ID,OUT_PATIENT_ID,INP_NO,PERSON_NAME,SEX_NAME,DATE_OF_BIRTH,VISIT_TYPE_CODE");
//            //通过EID查询solr
//            Map<String, Object> docsMap = SolrUtil.querySolrDocList("patInfoCollection", qparams, params);
//            List<Map<String, Object>> docList = (List<Map<String, Object>>) docsMap.get("solrList");
//            for (Map<String, Object> doc : docList) {
//                String pid = (String) (StringUtils.isBlank((String) doc.get("IN_PATIENT_ID"))
//                        ? doc.get("OUT_PATIENT_ID") : doc.get("IN_PATIENT_ID"));
//                String visit_type = (String) doc.get("VISIT_TYPE_CODE");
//                if (StringUtils.isNotBlank(pid) && StringUtils.isNotBlank(visit_type)) {
//                    pidInpNo.put(visit_type + "|" + pid, visit_type + "|" + pid);
//                }
//            }

            //通过EID查询solr
            ResultVO docsMap = SolrQueryUtils.querySolr(SolrUtils.patInfoCollection, q, null, new String[]{"IN_PATIENT_ID", "OUT_PATIENT_ID", "INP_NO", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "VISIT_TYPE_CODE"}, 1000, 1, null, null);
            List<Map<String, String>> docList = docsMap.getResult();
            for (Map<String, String> doc : docList) {
                String pid = (String) (StringUtils.isBlank((String) doc.get("IN_PATIENT_ID"))
                        ? doc.get("OUT_PATIENT_ID") : doc.get("IN_PATIENT_ID"));
                String visit_type = (String) doc.get("VISIT_TYPE_CODE");
                if (StringUtils.isNotBlank(pid) && StringUtils.isNotBlank(visit_type)) {
                    pidInpNo.put(visit_type + "|" + pid, visit_type + "|" + pid);
                }
            }
        }
        return pidInpNo;
    }

    @Override
    public Map<String, Object> getPatientVisitsInfo(String patientId, String visitType, String startTime,
                                                    String endTime, String dateType, String dept, String showList, String deptType) {
        //结果数据
        Map<String, Object> results = new HashMap<String, Object>();
        //查询当前患者信息
        Map<String, String> patientInfo = getInfoFromPatient(patientId, visitType);
        if ("0".equals(patientInfo.get("STATUS"))) { //未找到患者
            //返回空数组 便于前端判断
            List<Map<String, String>> emptyList = new ArrayList<Map<String, String>>();
            results.put("visit_list", emptyList);
            results.put("dept_list", emptyList);
            return results;
        }
        //查询此患者的所有pid
        Map<String, String> pidInpNo = getAllPids(patientInfo, patientId, visitType,deptType);

        //筛选当前患者的关联患者号，查询所有患者号对应的就诊信息
        List<Map<String, String>> visits = new ArrayList<Map<String, String>>();
        //就诊时间 科室 条件筛选
        boolean checkDate = ((StringUtils.isNotBlank(dateType) && !"allTime".equals(dateType))
                || (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)));
        boolean checkDept = (StringUtils.isNotBlank(dept) && !"allDept".equals(dept));

        //回显时间条件
        if ("allTime".equals(dateType)) {
            results.put("return_time", "" + "-" + DateUtils.getNowDate().replace("-", ""));
        }
        //根据时间条件筛选就诊信息
        String startDate = null;
        if (checkDate) {
            if (StringUtils.isNotBlank(dateType)) {
                startDate = Utils.calStartDate(dateType);
                results.put("return_time", startDate.replace("-", "") + "-" + DateUtils.getNowDate().replace("-", ""));
                //仅有一个时间
                visits = getAllVisits(pidInpNo, "1", startDate, null);
            } else {
                //有两个时间
                results.put("return_time", startTime.replace("-", "") + "-" + endTime.replace("-", ""));
                visits = getAllVisits(pidInpNo, "2", startTime, endTime);
            }
        } else {
            //无时间条件
            visits = getAllVisits(pidInpNo, "0", null, null);
        }

        //处理就诊数据  统计科室
        Map<String, Object> map = this.getDeptsByVisits(visits, dept,pidInpNo);

        //获取科室统计后的就诊数据  进行部门条件筛选
        List<Map<String, String>> visits2 = (List<Map<String, String>>) map.get("visit_list");

        //统计完科室后  再按部门条件筛选  保证科室统计不受科室条件影响
        if (checkDept) {
            Iterator<Map<String, String>> itor = visits2.iterator();
            while (itor.hasNext()) {
                Map<String, String> temp = itor.next();
                //如果本次就诊科室和前端界面选择的科室不一致  不符合科室条件 删除
                String dt = temp.get("dept_code");
                if (dt != null && !dept.equals(dt)) {
                    itor.remove();
                }
            }
        }
        //查询就诊视图是否显示就诊次
        String isViewVid = Config.getVISIT_SHOW_VID_CONFIG();
        //将上述条件筛选过的就诊数据  按时间排序
        List<Map<String, String>> sortedVisits = UpvUtil.sortMapList(visits2, "start_time");
        results.put("visit_list", sortedVisits);
        results.put("dept_list", map.get("dept_list"));
        results.put("vid_show_config", isViewVid);
        return results;
    }

    /**
     * @param visits  就诊列表
     * @param nowDept 当前选中的科室
     * @return
     * @Description 方法描述: 对就诊列表进行整理，统计科室
     */
    private Map<String, Object> getDeptsByVisits(List<Map<String, String>> visits, String nowDept,Map<String, String> pidInpNo) {
        //首先查出所有的过敏数据
        List<String> pids = new ArrayList<>();
        for(Entry<String,String> entry : pidInpNo.entrySet()){
            String value = StringUtils.isNotBlank(entry.getValue())?entry.getValue():"";
            String[] values = value.split("\\|");
            if(values.length == 2 ) {
                pids.add(values[1]);
            }
        }
        Map<String, String> allergyMap = getAllergys(pids, "", "");
        //结果数据
        Map<String, Object> resultMap = new HashMap<String, Object>();
        //科室信息   科室编码->科室名称
        Map<String, String> deptMap = new HashMap<String, String>();
        //就诊列表
        List<Map<String, String>> visitList = new LinkedList<Map<String, String>>();
        //遍历就诊列表 统计科室
        for (Map<String, String> visit : visits) {
            //存储当前就诊
            Map<String, String> nowVisitMap = new HashMap<String, String>();
            //就诊类型
            String visitType = visit.get("VISIT_TYPE");
            if ("OUTPV".equals(visitType)) {
                nowVisitMap.put("type_code", "OUTPV");
                nowVisitMap.put("type", "门诊");
            } else if ("INPV".equals(visitType)) {
                nowVisitMap.put("type_code", "INPV");
                nowVisitMap.put("type", "住院");
            } else if ("EMPV".equals(visitType)) {
                nowVisitMap.put("type_code", "EMPV");
                nowVisitMap.put("type", "急诊");
            }

            //判断本次就诊是否有过敏反应
            String vtype = "";
            if ("INPV".equals(visitType)) {
                vtype = "02";
            } else {
                vtype = "01";
            }
//            Map<String, Object> allergyMap = getAllergys(visit.get("NOW_PATIENT"), visit.get("VISIT_ID"), vtype);
            //保存判断结果
            nowVisitMap.put("allergy", allergyMap.get(visit.get("NOW_PATIENT")+"|"+visit.get("VISIT_ID").trim()));
            //开始  结束时间
            String startTime = visit.get("START_TIME");
            String endTime = visit.get("END_TIME");
            if (StringUtils.isNotBlank(startTime)) {
                nowVisitMap.put("start_time", startTime);
            } else {
                nowVisitMap.put("start_time", "-");
            }
            if (StringUtils.isNotBlank(endTime)) {
                nowVisitMap.put("end_time", endTime);
            } else {
                nowVisitMap.put("end_time", "-");
            }
            //就诊次数
            Utils.checkAndPutToMap(nowVisitMap, "visit_id", visit.get("VISIT_ID").trim(), "-", false);
            //患者编号
            Utils.checkAndPutToMap(nowVisitMap, "now_patient", visit.get("NOW_PATIENT"), "-", false);
            //住院号
            Utils.checkAndPutToMap(nowVisitMap, "inp_no", visit.get("INP_NO"), "-", false);
            //诊断信息
            Utils.checkAndPutToMap(nowVisitMap, "diagnosis_code", visit.get("DIAGNOSIS_CODE"), "", false);
            Utils.checkAndPutToMap(nowVisitMap, "diagnosis_name", visit.get("DIAGNOSIS_NAME"), "", false);
            Utils.checkAndPutToMap(nowVisitMap, "visit_no", visit.get("VISIT_NO"), "", false);
            Utils.checkAndPutToMap(nowVisitMap, "district_admission_to_name", visit.get("DISTRICT_ADMISSION_TO_NAME"), "", false);

            //统计科室 和 各科室就诊次数
            //默认取出出院科室
            String deptName = visit.get("END_DEPT_NAME");
            String deptCode = visit.get("END_DEPT_CODE");
            if (StringUtils.isNotBlank(deptCode)) { //出院科室编码不为空
                if (StringUtils.isBlank(deptName)) { //出院科室名称为空
                    //查询 MySQL，获取出院科室名称
                    Dept d1 = deptDao.getDeptByDeptcode(deptCode);
                    if (null != d1) {
                        deptName = d1.getDeptname();
                    } else { //未找到出院科室名，再获取入院科室
                        deptName = visit.get("START_DEPT_NAME");
                        deptCode = visit.get("START_DEPT_CODE");
                        if (StringUtils.isNotBlank(deptCode)) { //入院科室编码不为空
                            if (StringUtils.isBlank(deptName)) { //入院科室名称为空
                                //查询MYSQL，获取入院科室名称
                                Dept d2 = deptDao.getDeptByDeptcode(deptCode);
                                if (null != d2) {
                                    deptName = d2.getDeptname();
                                } else { //出院科室名称未知，入院科室名称未知，本次就诊科室标记 未知科室
                                    deptName = "未知科室";
                                    deptCode = deptName;
                                }
                            }
                        } else { //出院科室名称未知，入院科室编码未找到
                            deptName = "未知科室";
                            deptCode = deptName;
                        }
                    }

                }
            } else { //出院科室编码未找到
                //取出入院科室
                deptName = visit.get("START_DEPT_NAME");
                deptCode = visit.get("START_DEPT_CODE");
                if (StringUtils.isNotBlank(deptCode)) {
                    if (StringUtils.isBlank(deptName)) {
                        //查询 MySQL，获取入院科室名称
                        Dept d3 = deptDao.getDeptByDeptcode(deptCode);
                        if (null != d3) {
                            deptName = d3.getDeptname();
                        } else {
                            deptName = "未知科室";
                            deptCode = deptName;
                        }
                    }
                } else { //入院和出院科室编码均未找到
                    deptName = "未知科室";
                    deptCode = deptName;
                }
            }
            //保存当前就诊 科室编码 和 名称
            nowVisitMap.put("dept_code", deptCode);
            nowVisitMap.put("dept_name", deptName);
            //保存门诊状态
            nowVisitMap.put("visit_status", visit.get("VISIT_STATUS_NAME"));
            //保存科室  去除重复
            if (null != deptCode && !deptMap.containsKey(deptCode)) {
                deptMap.put(deptCode, deptName);
            }
            //保存处理后的当前就诊信息
            visitList.add(nowVisitMap);
        }

        //科室列表
        List<Map<String, String>> deptList = new LinkedList<Map<String, String>>();
        //遍历科室  统计各科室的就诊次数
        int tm = 0; //门诊总次数
        int tz = 0; //住院总次数
        int tj = 0; //急诊总次数
        int i = 0; //计数器，用于科室排序
        for (Entry<String, String> entry : deptMap.entrySet()) {
            i++;
            Map<String, String> map = new HashMap<String, String>();
            //标记当前选中的科室
            if (entry.getKey().equals(nowDept)) {
                map.put("now_dept", "true");
            }
            map.put("name", entry.getValue()); //科室名
            map.put("code", entry.getKey()); //科室编码
            //统计属于当前科室的  门诊 住院  急诊次数
            int m = 0; //门诊次数
            int z = 0; //住院次数
            int j = 0; //急诊次数
            for (Map<String, String> v : visitList) {
                String deptCode = v.get("dept_code");
                String visitType = v.get("type_code");
                if (deptCode.equals(entry.getKey())) {
                    if ("OUTPV".equals(visitType)) {
                        m++;
                    } else if ("INPV".equals(visitType)) {
                        z++;
                    } else if ("EMPV".equals(visitType)) {
                        j++;
                    }
                }
            }
            tm = tm + m;
            tz = tz + z;
            tj = tj + j;
            map.put("outpv", String.valueOf(m)); //门诊
            map.put("inpv", String.valueOf(z)); //住院
            map.put("emv", String.valueOf(j)); //急诊
            map.put("no", String.valueOf(i)); //序号
            if (!deptList.contains(map)) {
                deptList.add(map);
            }
        }
        //全部科室就诊次统计
        Map<String, String> tmap = new HashMap<String, String>();
        tmap.put("name", "全部科室");
        tmap.put("code", "allDept");
        tmap.put("outpv", String.valueOf(tm));
        tmap.put("inpv", String.valueOf(tz));
        tmap.put("emv", String.valueOf(tj));
        tmap.put("no", String.valueOf(deptMap.size() + 1));
        if ("allDept".equals(nowDept)) {
            tmap.put("now_dept", "true");
        }
        //序号降序排列，保证全部科室始终位于第一位
        Utils.sortListMulti(deptList, new String[]{"no"}, new String[]{"desc"});
        List<Map<String, String>> deList = new ArrayList<Map<String, String>>();
        deList.add(tmap);
        for (int j = 0; j < deptList.size(); j++) {
            deList.add(deptList.get(j));
        }
        //保存科室列表
        resultMap.put("dept_list", deList);
        //保存就诊列表
        resultMap.put("visit_list", visitList);

        return resultMap;
    }

    @Override
    public String getFinalInVisit(String patientId, String outPatientId) {
        // TODO Auto-generated method stub
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        //仅查询入出院记录
        filters.add(new PropertyFilter("TRANS_NO", PropertyType.INT.name(), MatchType.EQ.getOperation(), "0"));
        List<Map<String, String>> patAdts = hbaseDao.findConditionByKey(HdrTableEnum.HDR_PAT_ADT.getCode(),
                patientId, filters, new String[]{"VISIT_ID", "ADMISSION_TIME", "PERSON_NAME", "SEX_NAME",
                        "DATE_OF_BIRTH", "CURR_BED_LABEL", "CURR_DEPT_NAME", "ADMISSION_TIME", "INP_NO"});
        //关联患者号查询
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] ids = outPatientId.split(",");
            for (String pid : ids) {
                List<Map<String, String>> otherPatAdts = hbaseDao.findConditionByPatient(
                        HdrTableEnum.HDR_PAT_ADT.getCode(), pid, filters,
                        new String[]{"VISIT_ID", "ADMISSION_TIME", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH",
                                "CURR_BED_LABEL", "CURR_DEPT_NAME", "ADMISSION_TIME", "INP_NO"});
                //合并
                patAdts.addAll(otherPatAdts);
            }
        }
        //筛选末次住院信息，记录必要字段
        long resultInt = 1;
        String admissionTimeString = "";
        for (Map<String, String> map : patAdts) {
            long visitId = map.get("VISIT_ID").trim() == null ? 0 : Long.valueOf(map.get("VISIT_ID").trim());
            String adtString = map.get("ADMISSION_TIME") == null ? "" : map.get("ADMISSION_TIME");
            if (adtString.compareTo(admissionTimeString) > 0) {
                admissionTimeString = adtString;
                resultInt = visitId;
            }
        }
        return resultInt + "";
    }

    @Override
    public Map<String, String> getPatientInFinalVisit(String patientId, String outPatientId, String visitType) {
        Map<String, String> rs = new HashMap<String, String>();

        List<String> pats = new ArrayList<String>();
        pats.add(visitType + "|" + patientId);
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] ids = outPatientId.split(",");
            for (int i = 0; i < ids.length; i++) {
                pats.add(ids[i]);
            }
        }
        List<Map<String, String>> visitList = new ArrayList<Map<String, String>>();

        for (String pat : pats) {
            String[] info = pat.split("\\|");
            String type = info[0];
            String id = info[1];
            List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
            if ("02".equals(type)) {
                filters.add(new PropertyFilter("TRANS_NO", PropertyType.INT.name(), MatchType.EQ.getOperation(), "0")); //仅查询入出院记录
                List<Map<String, String>> inVisits = hbaseDao.findConditionByKey(
                        HdrTableEnum.HDR_PAT_ADT.getCode(), id, filters,
                        new String[]{"VISIT_ID", "ADMISSION_TIME", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH",
                                "VISIT_TYPE_CODE", "CURR_BED_LABEL", "CURR_DEPT_NAME", "CURR_DEPT_CODE",
                                "ADMISSION_TIME", "INP_NO"});
                for (Map<String, String> map : inVisits) {
                    map.put("VISIT_TYPE_CODE", "02");
                    map.put("PATIENT_ID", id);
                    visitList.add(map);
                }
            } else {
                continue;
            }
        }

        String visitTimeString = "";
        String patString = "";
        long resultInt = 1;
        for (Map<String, String> map : visitList) {
            patString = map.get("PATIENT_ID");
            long visitId = map.get("VISIT_ID").trim() == null ? 0 : Long.valueOf(map.get("VISIT_ID").trim());
            String adtString = map.get("ADMISSION_TIME") == null ? "" : map.get("ADMISSION_TIME");
            if (adtString.compareTo(visitTimeString) > 0) {
                visitTimeString = adtString;
                resultInt = visitId;
            }
        }

        if (StringUtils.isNotBlank(patString) && StringUtils.isNotBlank(resultInt + "")) {
            rs.put("patientId", patString);
            rs.put("visitId", resultInt + "");
        }

        return rs;
    }

    /**
     * 通过pid，vid,visitType获取就诊信息
     *
     * @param patientId    患者编号
     * @param  关联的患者编号
     * @param visitType    就诊类型
     * @return
     */
    public Map<String, String> getPatientInfo(String patientId, String visitId, String visitType) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        List<Map<String, String>> visitList = new ArrayList<Map<String, String>>();
        if ("02".equals(visitType)) {
            //仅查询入出院记录
            filters.add(new PropertyFilter("TRANS_NO", PropertyType.INT.name(), MatchType.EQ.getOperation(), "0"));
            filters.add(new PropertyFilter("VISIT_TYPE_CODE", PropertyType.STRING.name(), MatchType.EQ.getOperation(), visitType));
//			filters.add(new PropertyFilter("VISIT_ID", PropertyType.STRING.name(),
//					MatchType.EQ.getOperation(), visitId));
            List<Map<String, String>> inVisits = hbaseDao.findConditionByPatientVisitId(
                    HdrTableEnum.HDR_PAT_ADT.getCode(), patientId, visitId, filters,
                    new String[]{"VISIT_ID", "ADMISSION_TIME", "PERSON_NAME", "SEX_NAME", "IN_PATIENT_ID",
                            "OUT_PATIENT_ID", "DATE_OF_BIRTH", "VISIT_TYPE_CODE", "CURR_BED_LABEL",
                            "DEPT_ADMISSION_TO_CODE", "DEPT_ADMISSION_TO_NAME", "CURR_DEPT_NAME", "CURR_DEPT_CODE",
                            "ADMISSION_TIME", "INP_NO", "TRANS_NO", "DISTRICT_ADMISSION_TO_NAME", "DISCHARGE_TIME"});
            for (Map<String, String> map : inVisits) {
                map.put("VISIT_TYPE_CODE", "02");
                map.put("PATIENT_ID", patientId);
                visitList.add(map);
            }
        } else {
            filters.add(new PropertyFilter("VISIT_TYPE_CODE", PropertyType.STRING.name(), MatchType.EQ.getOperation(), visitType));
            filters.add(new PropertyFilter("VISIT_ID", PropertyType.STRING.name(),
                    MatchType.EQ.getOperation(), visitId));
            if (StringUtils.isNotBlank(Config.getCIV_OUT_VISIT_FILTER())) {
                strToFilter(Config.getCIV_OUT_VISIT_FILTER(), filters, ";");
            }
            List<Map<String, String>> outVisits = hbaseDao.findConditionByPatient(
                    HdrTableEnum.HDR_OUT_VISIT.getCode(), patientId, filters,
                    new String[]{"VISIT_TIME", "VISIT_ID", "VISIT_DEPT_NAME", "OUTP_NO", "IN_PATIENT_ID",
                            "OUT_PATIENT_ID", "VISIT_DEPT_CODE", "VISIT_TYPE_CODE", "PERSON_NAME", "SEX_NAME",
                            "DATE_OF_BIRTH", "VISIT_FLAG"});
            for (Map<String, String> map : outVisits) {
                map.put("VISIT_TYPE_CODE", "01");
                map.put("PATIENT_ID", patientId);
                visitList.add(map);
            }
        }

        //就诊去重：只留下转科情况的最后一次转科记录
        visitList = deleteMultiVisit(visitList);
        //新增脱敏
        powerService.getInfoHidden(visitList);
        String dischargeTime = "";
        Map<String, String> resMap = new HashMap<String, String>();
        for (Map<String, String> map : visitList) {
            dischargeTime = map.get("DISCHARGE_TIME");
            if ("02".equals(visitType)) {
                Utils.checkAndPutToMap(resMap, "admission_time", map.get("ADMISSION_TIME"), "-", false); //就诊时间
                Utils.checkAndPutToMap(resMap, "stay_bed", map.get("CURR_BED_LABEL"), "-", false); //床号
                Utils.checkAndPutToMap(resMap, "stay_dept_name", map.get("DEPT_ADMISSION_TO_NAME"), "-", false); //科室
                Utils.checkAndPutToMap(resMap, "patient_id", patientId, "-", false); //患者编号
                Utils.checkAndPutToMap(resMap, "inp_no", map.get("INP_NO"), "-", false); //住院号
            } else {
                Utils.checkAndPutToMap(resMap, "admission_time", map.get("VISIT_TIME"), "-", false); //就诊时间
                Utils.checkAndPutToMap(resMap, "stay_dept_name", map.get("VISIT_DEPT_NAME"), "-", false); //科室
                Utils.checkAndPutToMap(resMap, "patient_id", patientId, "-", false); //患者编号
                Utils.checkAndPutToMap(resMap, "inp_no", map.get("OUTP_NO"), "-", false); //住院号
            }
        }
        //末次住院主诊断
        //TODO 由于现场主诊断读取方式不一样，注释掉原方法，临时用新方法代替
        List<Map<String, String>> patDiagList = new ArrayList<Map<String, String>>();
        if ("02".equals(visitType)) {
            if (StringUtils.isBlank(dischargeTime)) {
                patDiagList = getPatMainDiagInpbyConfig(patientId, visitId);
            } else {
                patDiagList = getPatInpDiag(patientId, visitId);
            }
        } else {
            patDiagList = getPatDiagOutp(patientId, visitId, "1");
        }
        if (patDiagList.size() > 0) {
            Utils.checkAndPutToMap(resMap, "diagnosis_code", patDiagList.get(0).get("DIAGNOSIS_CODE"), "-", false);
            Utils.checkAndPutToMap(resMap, "diagnosis_name", patDiagList.get(0).get("DIAGNOSIS_NAME"), "-", false);
        } else {
            resMap.put("diagnosis_code", "-");
            resMap.put("diagnosis_name", "-");
        }
        resMap.put("visit_type_code", visitType);
        return resMap;
    }
    /**
     *  获取患者当前就诊的信息
     * @param patientId 患者编号
     * @param outPatientId 关联的患者编号
     * @param visitType 就诊类型
     */

    public Map<String, String> getCurrentPatientInfo(String patientId, String outPatientId, String visitType) {


    /* 前端传入的 patientId和outPatientId 可能为多个 #传入的 outPatientId 形如 visitType|patientId #，为每一个患者执行{
            如果 患者就诊类型等于 02 ，那么{
                增设查询条件 "VISIT_TYPE_CODE"= 02 ；
                根据 patientId ，结合所有查询条件，从 HDR_PAT_ADT 中查出字段：
                        {"VISIT_ID", "ADMISSION_TIME", "PERSON_NAME", "SEX_NAME", "IN_PATIENT_ID",
                        "OUT_PATIENT_ID", "DATE_OF_BIRTH", "VISIT_TYPE_CODE", "CURR_BED_LABEL",
                        "DEPT_ADMISSION_TO_CODE", "DEPT_ADMISSION_TO_NAME", "CURR_DEPT_NAME", "CURR_DEPT_CODE",
                        "ADMISSION_TIME", "INP_NO", "TRANS_NO", "DISTRICT_ADMISSION_TO_NAME", "DISCHARGE_TIME"}；
            }

            如果 患者就诊类型不等于 02 ，那么{
                读取civ_config表中 CIV_OUT_VISIT_FILTER 配置项，如果不为空，将配置项增设为查询条件；
                根据 patientId ，结合所有查询条件，从 HDR_OUT_VISIT 中查出字段：
                        {"VISIT_ID", "ADMISSION_TIME", "PERSON_NAME", "SEX_NAME", "IN_PATIENT_ID",
                        "OUT_PATIENT_ID", "DATE_OF_BIRTH", "VISIT_TYPE_CODE", "CURR_BED_LABEL",
                        "DEPT_ADMISSION_TO_CODE", "DEPT_ADMISSION_TO_NAME", "CURR_DEPT_NAME", "CURR_DEPT_CODE",
                        "ADMISSION_TIME", "INP_NO", "TRANS_NO", "DISTRICT_ADMISSION_TO_NAME", "DISCHARGE_TIME"}

            }


        }
    */
    /*  将查询结果作为参数调用 deleteMultiVisit 就诊去重：只留下转科情况的最后一次转科记录 # 取TRANS_NO最大的记录 #*/
    /*  @param visitList 查询结果列表*/
         deleteMultiVisit(visitList);

    //姓名，性别，出生日期空值处理

    /*     如果姓名、性别、出生日期任一项为空，那么{
            增设查询条件"VISIT_TYPE_CODE"=visitTypeCode；
            根据patientId,结合以上查询条件，从 HDR_PATIENT 中查出字段
            { "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "IN_PATIENT_ID", "OUT_PATIENT_ID" }

        }
    */
    //末次住院主诊断
    /*    如果"VISIT_TYPE_CODE" 等于02，那么{
            如果"DISCHARGE_TIME" 为空，那么 {
                调用 getPatMainDiagInpbyConfig 方法：
                @param patId 前端传入的patientId
                @param visid 查出来的visitId
    */
                getPatMainDiagInpbyConfig(patId, visitId)；
    /*
            }否则{
                调用 getPatInpDiag 方法：
                @param patId 前端传入的patientId
                @param visid 查出来的visitId
    */
                getPatInpDiag(patId,visitId);
    /*
                如果 getPatInpDiag 没有查到，再调用 getPatMainDiagInpbyConfig 方法：
                @param patId 前端传入的patientId
                @param visid 查出来的visitId
    */
                getPatMainDiagInpbyConfig(patId,visitId);
    /*
            }

        }
        如果"VISIT_TYPE_CODE" 不等于02，那么调用 getPatDiagOutp 方法：
            @param patId 前端传入的patientId
            @param visid 查出来的visitId
    */
            getPatDiagOutp(patId,visitId，1);
    /*
        将诊断信息映射给前端；

    */





    //统计患者过敏次数

    /*    从mysql中 civ_sys_config 表里 configcod 为 "StartUse_Allergy"的 configvalue 值；
        如果对应的 configvalue 值等于 1 ，那么{
            增设查询条件 "VISIT_TYPE_CODE" = type（前端传来的就诊类型）；
            根据 patientId 从 "HDR_ALLERGY" 中查出 "ALLERGY_CATEGORY_CODE" ；
            统计查询结果的数量作为过敏次数，返回给前端。
        }
    */


    }

    @Override
    public List<Map<String, String>> getPatDiagInp(String patId, String visitId, String diagType, String isMain) {
        //获取患者住院诊断:初步诊断、主诊断
        String tableName = "HDR_EMR_CONTENT_DIAG";
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        //诊断类型
        if (StringUtils.isNotBlank(diagType)) {
            filters.add(new PropertyFilter("DIAGNOSIS_PROPERTY_CODE", "STRING", MatchType.IN.getOperation(), diagType));
        }
        //是否为主诊断
        if (isMain.equals("1")) {
            //主诊断
            filters.add(new PropertyFilter("DIAGNOSIS_NUM", "STRING", MatchType.EQ.getOperation(), "1"));
            filters.add(new PropertyFilter("DIAGNOSIS_SUB_NUM", "STRING", MatchType.EQ.getOperation(), "0"));
        } else if (isMain.equals("2")) {
            //其他诊断
            filters.add(new PropertyFilter("DIAGNOSIS_NUM", "STRING", MatchType.NE.getOperation(), "1"));
        }

        List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(tableName, patId, visitId, filters,
                new String[]{"DIAGNOSIS_CODE", "DIAGNOSIS_NAME", "DIAGNOSIS_TIME", "DIAGNOSIS_DOCTOR_CODE",
                        "DIAGNOSIS_DOCTOR_NAME", "DIAGNOSIS_DESC"});
        return list;
    }

    @Override
    public List<Map<String, String>> getPatMainDiagInpbyConfig(String patId, String visitId) {
        //获取患者住院诊断:初步诊断、主诊断
        String tableName = "HDR_EMR_CONTENT_DIAG";
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();

        //TODO 增加配置读取患者主诊断
        CivUtils.strToFilter(Config.getCIV_PAT_MAINDIAG_INP_FILTER(), filters, ";");

        List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(tableName, patId, visitId, filters,
                new String[]{"DIAGNOSIS_CODE", "DIAGNOSIS_NAME", "DIAGNOSIS_TIME", "DIAGNOSIS_DOCTOR_CODE",
                        "DIAGNOSIS_DOCTOR_NAME", "DIAGNOSIS_DESC","VISIT_ID"});
        return list;
    }

    public List<Map<String, String>> getPatInpDiag(String patId, String visitId) {
        //获取患者住院诊断:初步诊断、主诊断
        String tableName = "HDR_INP_SUMMARY_DIAG";
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        CivUtils.strToFilter(Config.getCIV_PAT_MAINDIAG_INP_FILTER(), filters, ";");
        List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(tableName, patId, visitId, filters, new String[]{
                "DIAGNOSIS_CODE", "DIAGNOSIS_NAME", "DIAGNOSIS_TIME", "DIAGNOSIS_DESC", "DIAGNOSIS_NUM", "DIAGNOSIS_TYPE_NAME","VISIT_ID"});
        return list;
    }

    @Override
    public List<Map<String, String>> getPatDiagOutp(String patId, String visitId, String isMain) {
        //过滤条件
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if ("1".equals(isMain)) { //主诊断
            filters.add(new PropertyFilter("MAIN_FLAG", "STRING", MatchType.EQ.getOperation(), "1"));
        } else if ("2".equals(isMain)) { //非主诊断
            filters.add(new PropertyFilter("MAIN_FLAG", "STRING", MatchType.NE.getOperation(), "1"));
        }
        List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(
                HdrTableEnum.HDR_OUT_VISIT_DIAG.getCode(), patId, visitId, filters,
                new String[]{"DIAGNOSIS_CODE", "DIAGNOSIS_NAME", "DIAGNOSIS_TIME", "DIAGNOSIS_DOCTOR_CODE",
                        "DIAGNOSIS_DOCTOR_NAME", "DIAGNOSIS_DESC","VISIT_ID"});
        //诊断时间降序
        Utils.sortListByDate(list, "DIAGNOSIS_TIME", Sort.DESC);
        return list;
    }

    /**
     * @param
     * @param visitId   就诊次数
     * @param visitType 就诊类型  01|02
     * @return
     * @Description 方法描述: 判断患者某次就诊是否有过敏反应
     */
    private Map<String, String> getAllergys(List<String> pids , String visitId, String visitType) {
        Map<String, String> res = new HashMap<String, String>();
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if(StringUtils.isNotBlank(visitId)) {
            filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        }
        if(StringUtils.isNotBlank(visitType)) {
            filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
        }
        List<Map<String, String>> list  = new ArrayList<Map<String, String>>();
        for(String patientId : pids ) {
            if (StringUtils.isNotBlank(patientId)) {
                list.addAll(hbaseDao.findConditionByPatient("HDR_ALLERGY", patientId, filters,
                        "ALLERGY_CATEGORY_CODE", "VISIT_ID","IN_PATIENT_ID"));
            }
        }
        //将结果按pid+vid拼接处理
        for(Map<String,String> map : list){
            res.put(map.get("IN_PATIENT_ID") + "|" + map.get("VISIT_ID").trim(),"true");
        }
        return res;
    }

    /**
     * 字符查询传转换为filter查询对象
     *
     * @param filterString 查询字符串 column|in|080,079,004,035,088,134
     * @param filters
     * @param strSplit     分隔每个查询条件的分隔符
     */
    private static void strToFilter(String filterString, List<PropertyFilter> filters, String strSplit) {
        //将配置字符串转换为查询
        if (StringUtils.isNotBlank(filterString)) {
            String[] filterStrings = filterString.split(strSplit);
            for (String filterItemString : filterStrings) {
                String[] tempString = filterItemString.split("\\|");
                createPropertyFilter(tempString[0], tempString[2], tempString[1], filters);
            }
        }
    }

    /**
     * 创建Filter
     *
     * @param columnName
     * @param keyword
     * @param filters
     */
    public static void createPropertyFilter(String columnName, String keyword, String MatchType,
                                            List<PropertyFilter> filters) {
        if (StringUtils.isNotBlank(keyword)) {
            PropertyFilter filter1 = new PropertyFilter();
            filter1.setMatchType(MatchType);
            filter1.setPropertyName(columnName);
            filter1.setPropertyValue(keyword);
            filter1.setPropertyType("STRING");
            filters.add(filter1);
        }
    }

    /**
     * 存在转科的情况下去重
     *
     * @param list
     */
    public List<Map<String, String>> deleteMultiVisit(List<Map<String, String>> list) {
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        List<Map<String, String>> restult = new ArrayList<Map<String, String>>();
        for (Map<String, String> mapTemp : list) {
            String vid = mapTemp.get("VISIT_ID");
            String vtypeCode = mapTemp.get("VISIT_TYPE_CODE");
            String pid = StringUtils.isNotBlank(mapTemp.get("IN_PATIENT_ID")) ? mapTemp.get("IN_PATIENT_ID")
                    : mapTemp.get("OUT_PATIENT_ID");
            if (null == map.get(pid + "|" + vid + "|" + vtypeCode)) {
                map.put(pid + "|" + vid + "|" + vtypeCode, mapTemp);
            } else {
                String transNo = mapTemp.get("TRANS_NO");
                String transNoOld = map.get(pid + "|" + vid + "|" + vtypeCode).get("TRANS_NO");
                int trans_no = StringUtils.isNotBlank(transNo) ? Integer.parseInt(transNo) : 0;
                int trans_no_old = StringUtils.isNotBlank(transNoOld) ? Integer.parseInt(transNoOld) : 0;
                if (trans_no_old < trans_no) {
                    map.put(pid + "|" + vid + "|" + vtypeCode, mapTemp);
                }
            }
        }
        for (String key : map.keySet()) {
            Map<String, String> mapTemp = map.get(key);
            restult.add(mapTemp);
        }
        return restult;
    }

    /**
     * 根据身份证号+患者姓名关联免登陆跳转地址
     *
     * @param id_crad_no
     * @param personName
     * @param userName
     * @return
     */
    @Override
    public Map<String, String> getPatientinfo(String id_crad_no, String personName, String userName) {
        Map<String, String> result = new HashMap<String, String>();
        //检索solr，根据身份证号关联患者末次就诊信息(缺一不可)
        if (StringUtils.isBlank(id_crad_no) || StringUtils.isBlank(personName)) {
            result.put("stutas", "0");
            return result;
        }
        String url = "";
        //通过EID查询solr
        ResultVO docsMap = SolrQueryUtils.querySolr(SolrUtils.patInfoCollection, "ID_CARD_NO:" + id_crad_no + " AND PERSON_NAME:" + personName , null, new String[]{"IN_PATIENT_ID,VISIT_ID,OUT_PATIENT_ID,VISIT_TYPE_CODE"}, 1000, 1, null, null);
        List<Map<String, String>> docList = docsMap.getResult();
        for (Map<String, String> doc : docList) {
            String pid = StringUtils.isBlank(doc.get("IN_PATIENT_ID")) ? doc.get("OUT_PATIENT_ID")
                    : doc.get("IN_PATIENT_ID");
            String visit_type = ("01").equals(doc.get("VISIT_TYPE_CODE"))? "01" : "02";
            String visit_id =  doc.get("VISIT_ID").trim();
            url = Config.getCIV_DEFAULT_PAGE() + "/rpc?username=" + userName + "&visit_id=" + visit_id + "&visit_type="
                    + visit_type + "&patient_id=" + pid + "&way=en";
            result.put("stutas", "1");
            result.put("URL", url);
        }
        if (result.isEmpty()) {
            result.put("stutas", "0");
        }
        return result;
    }

}
