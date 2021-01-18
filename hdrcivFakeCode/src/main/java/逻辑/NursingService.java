package 逻辑;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

public class NursingService {


    @Autowired
    private HbaseDao hbaseDao;


    public Map<String, String> getNursingUrl(String patientId, String visitId) {
        // TODO Auto-generated method stub
        Map<String, String> rs = new HashMap<String, String>();
        String project = NursingConfig.getProject_site();
        if (StringUtils.isBlank(project)) {
            return rs;
        }
        String url = NursingConfig.getURL();
        if (StringUtils.isBlank(url)) {
            rs.put("status", "0");
            rs.put("msg", "请联系管理员，配置护理记录调用地址。");
            return rs;
        }
        //南医三院特殊处理
        if (HdrConstantEnum.HOSPITAL_NYSY.getCode().equals(ConfigCache.getCache("org_oid"))) {
            String res = getNYSYNurseUrl(url, patientId, visitId, new HashMap<String, String>());
            rs.put("status", "1");
            rs.put("msg", "获取URL成功");
            rs.put("url", res);
            return rs;
        }
        List<String> params = NursingConfig.getParams();
        Map<String, String> config = NursingConfig.getParam_configs();
        Map<String, String> values = getParamValue(patientId, visitId, config);
        for (int i = 0; i < params.size(); i++) {
            String field = params.get(i);
            if (StringUtils.isNotBlank(values.get(field))) {
                url = url.replace("#{" + field + "}", values.get(field));
            } else {
                rs.put("status", "0");
                rs.put("msg", "请联系管理员，" + field + "参数为空。");
                return rs;
            }
        }
        rs.put("status", "1");
        rs.put("url", url);
        return rs;
    }


    private Map<String, String> getParamValue(String patientId, String visitId, Map<String, String> configs) {
        // TODO Auto-generated method stub
        Map<String, String> rs = new HashMap<String, String>();
        for (String key : configs.keySet()) {
            String[] config = configs.get(key).split(",");
            if ("IN_PATIENT_ID".equals(config[0])) {
                rs.put(key, patientId);
            }
            if ("VISIT_ID".equals(config[0])) {
                rs.put(key, visitId);
            }
            List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(config[1], patientId, visitId, new ArrayList<PropertyFilter>(), config[0]);
            if (list.size() > 0) {
                rs.put(key, list.get(0).get(config[0]));
            }
        }
        return rs;
    }

    /**
     * 南医三院护理url特殊处理
     *
     * @param url
     * @param pid
     * @param vid
     * @return
     */
    private String getNYSYNurseUrl(String url, String pid, String vid, Map<String, String> paramMap) {
        return GenUtils.setUrl(url, pid, vid, paramMap);
    }


    public List<Map<String, String>> getAllINVisits(String patientId, String visitType, String year) {
        // TODO Auto-generated method stub
        //就诊列表
        List<Map<String, String>> visits = new ArrayList<Map<String, String>>();
        if (StringUtils.isBlank(year)) {
            Calendar date = Calendar.getInstance();
            year = String.valueOf((date.get(Calendar.YEAR) + 1));
        }
        String visitPid = visitType + "|" + patientId;
        //key:就诊类型|门诊和住院患者编号    value:就诊类型
        Map<String, String> pidInpNo = new HashMap<String, String>();
        //先放入当前传入的就诊类型 和 患者编号
        pidInpNo.put(visitPid, visitPid);

        //根据主索引查询 Solr 处理门诊患者标识和住院患者标识不一致情况
        Map<String, String> patInfos = getInfoFromPatient(patientId, visitType);
        String EID = patInfos.get("EID");
        if (StringUtils.isNotBlank(EID) && !"EID未知".equals(EID)) {
            //查询条件
            String q = "*:* AND ";
            q += "EID:" + EID;
            //通过EID查询solr
            ResultVO docsMap = SolrQueryUtils.querySolr(SolrUtils.patInfoCollection, q, null, new String[]{"IN_PATIENT_ID", "OUT_PATIENT_ID", "INP_NO", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "VISIT_TYPE_CODE"}, 10000, 1, null, null);
            List<Map<String, String>> docList = docsMap.getResult();
            for (Map<String, String> doc : docList) {
                String pid = (String) (doc.get("IN_PATIENT_ID") == null ? doc.get("OUT_PATIENT_ID") : doc.get("IN_PATIENT_ID"));
                String visit_type = (String) doc.get("VISIT_TYPE_CODE");
                if (StringUtils.isNotBlank(pid) && StringUtils.isNotBlank(visit_type)) {
                    pidInpNo.put(visit_type + "|" + pid, visit_type + "|" + pid);
                }
            }
        }
        int num = 0;
        while (num < 20) {
            year = (Integer.valueOf(year) - 1) + "";
            //根据上述筛选后的患者编号和就诊类型   获取所有就诊列表
            for (Map.Entry<String, String> entry : pidInpNo.entrySet()) {
                String[] vtpid = entry.getKey().split("\\|");
                if ("02".equals(vtpid[0])) {
                    getVisitsAndAddToVisits(visits, vtpid[1], year);
                }
            }
            num++;
            if (num == 20)
                break;
        }
        //按时间排序
        Utils.sortListByDate(visits, "START_TIME", "desc");
        return visits;
    }


    public List<Map<String, String>> getAllINVisitsForOper(String patientId, String visitType, String year) {
        // TODO Auto-generated method stub
        //就诊列表
        List<Map<String, String>> visits = new ArrayList<Map<String, String>>();
        if (StringUtils.isBlank(year)) {
            Calendar date = Calendar.getInstance();
            year = String.valueOf((date.get(Calendar.YEAR) + 1));
        }
        String visitPid = visitType + "|" + patientId;
        //key:就诊类型|门诊和住院患者编号    value:就诊类型
        Map<String, String> pidInpNo = new HashMap<String, String>();
        //先放入当前传入的就诊类型 和 患者编号
        pidInpNo.put(visitPid, visitPid);

        //根据主索引查询 Solr 处理门诊患者标识和住院患者标识不一致情况
        Map<String, String> patInfos = getInfoFromPatient(patientId, visitType);
        String EID = patInfos.get("EID");
        if (StringUtils.isNotBlank(EID) && !"EID未知".equals(EID)) {
            //查询条件
            String q = "*:* AND ";
            q += "EID:" + EID;
            //通过EID查询solr
            ResultVO docsMap = SolrQueryUtils.querySolr(SolrUtils.patInfoCollection, q, null, new String[]{"IN_PATIENT_ID", "OUT_PATIENT_ID", "INP_NO", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "VISIT_TYPE_CODE"}, 10000, 1, null, null);
            List<Map<String, String>> docList = docsMap.getResult();
            for (Map<String, String> doc : docList) {
                String pid = (String) (doc.get("IN_PATIENT_ID") == null ? doc.get("OUT_PATIENT_ID") : doc.get("IN_PATIENT_ID"));
                String visit_type = (String) doc.get("VISIT_TYPE_CODE");
                if (StringUtils.isNotBlank(pid) && StringUtils.isNotBlank(visit_type)) {
                    pidInpNo.put(visit_type + "|" + pid, visit_type + "|" + pid);
                }
            }
        }
        int num = 0;
        while (num != 21) {
            year = (Integer.valueOf(year) - 1) + "";
            //根据上述筛选后的患者编号和就诊类型   获取所有就诊列表
            for (Map.Entry<String, String> entry : pidInpNo.entrySet()) {
                String[] vtpid = entry.getKey().split("\\|");
//                if ("02".equals(vtpid[0])) {
                getVisitsAndAddToVisits(visits, vtpid[1], year);
//                }
            }
            num++;
            if (num == 20)
                break;
        }
        return visits;
    }

    /**
     * @param visits    存储就诊列表的集合
     * @param patientId 患者编号
     * @param visitType 就诊类型
     * @return 返回类型： void
     * @Description 方法描述: 根据患者编号和就诊类型查询该类型下的就诊列表，并将数据填充到visits
     */
    private void getVisitsAndAddToVisits(List<Map<String, String>> visits, String patientId, String year) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        filters.add(new PropertyFilter("ADMISSION_TIME", "STRING", MatchType.GE.getOperation(), year + "-01-01 00:00:00"));
        filters.add(new PropertyFilter("ADMISSION_TIME", "STRING", MatchType.LE.getOperation(), year + "-12-31 23:59:59"));
        //住院
        filters.add(new PropertyFilter("TRANS_NO", "STRING", MatchType.EQ.getOperation(), "0")); //仅取入出院，不取转科
        List<Map<String, String>> patAdts = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_PAT_ADT.getCode(),
                patientId, filters, new String[]{"DEPT_DISCHARGE_FROM_CODE", "DEPT_DISCHARGE_FROM_NAME",
                        "ADMISSION_TIME", "DISCHARGE_TIME", "VISIT_ID", "DEPT_ADMISSION_TO_NAME",
                        "DEPT_ADMISSION_TO_CODE", "INP_NO", "IN_PATIENT_ID"});
        for (Map<String, String> adt : patAdts) {
            Map<String, String> covert = new HashMap<String, String>();
            covert.put("END_DEPT_CODE", adt.get("DEPT_DISCHARGE_FROM_CODE")); //出院科室
            covert.put("END_DEPT_NAME", adt.get("DEPT_DISCHARGE_FROM_NAME"));
            covert.put("START_DEPT_NAME", adt.get("DEPT_ADMISSION_TO_NAME")); //入院科室
            covert.put("START_DEPT_CODE", adt.get("DEPT_ADMISSION_TO_CODE"));
            covert.put("START_TIME", adt.get("ADMISSION_TIME")); //入院时间
            covert.put("END_TIME", adt.get("DISCHARGE_TIME")); //出院时间
            covert.put("VISIT_ID", adt.get("VISIT_ID")); //就诊次数
            //查询本次住院诊断  主诊断
            String visitId = adt.get("VISIT_ID");
            if (!covert.isEmpty()) {
                covert.put("INP_NO", adt.get("INP_NO"));
                covert.put("VISIT_TYPE", "INPV");
                covert.put("NOW_PATIENT", adt.get("IN_PATIENT_ID"));//区分传入的患者编号和通过EID关联出来的患者编号
                visits.add(covert);
            }
        }
    }


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

    /**
     * 统计护理类型
     *
     * @param patientId
     * @param visitId
     * @return
     */

    public List<Map<String, String>> getNurseTypes(String patientId, String visitId, String visitType, String outPatientId) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if (StringUtils.isNotBlank(visitId)) {
            filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        }
        if (StringUtils.isNotBlank(visitType)) {
            filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
        }
        System.out.println("--------------------------");
        for (PropertyFilter f : filters) {
            System.out.println(f.getPropertyName());
            System.out.println(f.getMatchType());
            System.out.println(f.getPropertyType());
            System.out.println(f.getPropertyValue());
            System.out.println("===============");
        }
        System.out.println("--------------------------");
        List<Map<String, String>> list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_NURSE_CONTENT.getCode(),
                patientId, filters, new String[]{"NR_CODE", "NR_NAME"});
        //outPatientId
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] outPids = outPatientId.split(",");
            for (String outPid : outPids) {
                String[] pid_vid = outPid.split("\\|");
                if (!patientId.equals(pid_vid[1]) && !visitType.equals(pid_vid[0])) {
                    List<Map<String, String>> list2 = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_NURSE_CONTENT.getCode(),
                            pid_vid[1], filters, new String[]{"NR_CODE", "NR_NAME"});
                    list.addAll(list2);
                }
            }
        }
        System.out.println("-------------::::" + list.size());
        List<Map<String, String>> res = new ArrayList<Map<String, String>>();
        Map<String, String> all = new HashMap<String, String>();
        all.put("code", "all");
        all.put("name", "全部");
        res.add(all);
        Set<String> set = new HashSet<String>();
        for (Map<String, String> map : list) {
            Map<String, String> temp = new HashMap<String, String>();
            if (StringUtils.isBlank(map.get("NR_NAME")) || StringUtils.isBlank(map.get("NR_CODE"))) {
                continue;
            }
            if (set.contains(map.get("NR_CODE"))) {
                continue;
            }
            set.add(map.get("NR_CODE"));
            temp.put("code", map.get("NR_CODE"));
            temp.put("name", map.get("NR_NAME"));
            res.add(temp);
        }
        return res;
    }

    /**
     * 护理列表
     *
     * @param patientId    患者id
     * @param visitId      就诊次数
     * @param visitType    就诊类型
     * @param outPatientId 门诊患者id
     * @param nurseType    护理类型
     * @param pageNo       当前页码
     * @param pageSize     每页大小
     */

    public void getNurseList(String patientId, String visitId, String visitType, String outPatientId, String nurseType, int pageNo, int pageSize) {


      /*  构建查询条件

       如果 nurseType 不为空且不等于 “all” ,增设查询条件为 "NR_CODE" = nurseType;
       如果 visitId 不为空，增设查询条件为 "VISIT_ID" = visitId ;
      */

       /*  结合所有查询条件，从HBase中 HDR_NURSE_CONTENT 表查询患者id为 patientId 的记录中 "LAST_MODIFY_DATE_TIME", "NR_NAME" 列。*/

       /*  如果 outPatientId 不为空，那么 括号1{
                先以 ',' 为分隔符，将传入的 outPatientId 拆分为数组，对数组每一项进行循环  括号2{
                    以 '|' 为分隔符，继续拆为2部分，
                    如果 第2部分不等于传入的patientId，且第1部分不等于visitType，那么 括号3{
                        结合前面构造的查询条件，查询 HDR_NURSE_CONTENT 表，以第2部分为患者id的记录中 "LAST_MODIFY_DATE_TIME", "NR_NAME" 列
                    }括号3
                }括号2
            }括号1
      */


    }

    /**
     * 获取护理单表头
     *
     * @return
     */
    @Override
    public List<Map<String, String>> getNurseTableHead() {
        List<Map<String, String>> head = Config.getCIV_NURSE_TABLE_HEAD();
        return head;
    }

    /**
     * 获取护理单详情
     *
     * @param
     */
    @Override
    public Page<Map<String, String>> getNurseDetail(String patientId, String outPatientId, String date, int pageNo, int pageSize) {
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (0 == pageNo) {
            pageNo = 1;
        }
        if (0 == pageSize) {
            pageSize = 10;
        }
        page.setOrderBy("LAST_MODIFY_DATE_TIME");
        page.setOrderDir("desc");
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if (StringUtils.isNotBlank(date)) {
            filters.add(new PropertyFilter("LAST_MODIFY_DATE_TIME", "STRING", MatchType.GE.getOperation(), date + " 00:00:00"));
            filters.add(new PropertyFilter("LAST_MODIFY_DATE_TIME", "STRING", MatchType.LE.getOperation(), date + " 23:59:59"));
        }
        List<Map<String, String>> list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_NURSE_CONTENT.getCode(), patientId,
                filters, Config.getCIV_NURSE_TABLE_FIELD());
        //outPatientId
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] outPids = outPatientId.split(",");
            for (String outPid : outPids) {
                String[] pid_vid = outPid.split("\\|");
                if (!patientId.equals(pid_vid[1])) {
                    List<Map<String, String>> list2 = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_NURSE_CONTENT.getCode(),
                            pid_vid[1], filters, Config.getCIV_NURSE_TABLE_FIELD());
                    list.addAll(list2);
                }
            }
        }
        page.setTotalCount(list.size());
        //首先通过日期排序
        Utils.sortListByDate(list, "LAST_MODIFY_DATE_TIME", "desc");
        ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(list, pageNo, pageSize);
        List<Map<String, String>> item = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : listPage.getPagedList()) {
            Map<String, String> tempMap = new HashMap<String, String>();
            //处理html,PDF和url
            if ("HTML".equals(map.get("FFILE_CLASS"))) {
                tempMap.put("linkType", "HTML");
            } else if ("PDF".equals(map.get("FFILE_CLASS"))) {
                tempMap.put("linkType", "PDF");
                //处理PDF数据为文件
                map.put("URL", CivUtils.getPdfData(map.get("NR_CONTENT_HTML"), patientId, "0", DateUtils.getDateTime(new Date())));
            } else if ("URL".equals(map.get("FFILE_CLASS"))) {
                tempMap.put("linkType", "URL");
            } else {
                tempMap.put("linkType", "HTML");
            }
            //字段映射
            ColumnUtil.convertMapping(tempMap, map, Config.getCIV_NURSE_TABLE_FIELD());
            item.add(tempMap);
        }
        page.setResult(item);
        return page;
    }

    /**
     * 护理数量
     *
     * @param patientId
     * @param visitId
     * @return
     */
    @Override
    public Object getNurseNum(String patientId, String visitId, String visitType, String outPatientId, String nurseType, int pageNo, int pageSize) {
        if (StringUtils.isNotBlank(CommonConfig.getURL("NURSE")) &
                StringUtils.isNotBlank(CommonConfig.getURL("IN_NURSE")) &
                StringUtils.isNotBlank(CommonConfig.getURL("OUT_NURSE"))) {
            return "";
        }
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if (StringUtils.isNotBlank(nurseType) && !"all".equals(nurseType)) {
            filters.add(new PropertyFilter("NR_CODE", "STRING", MatchType.EQ.getOperation(), nurseType));
        }
        if (StringUtils.isNotBlank(visitId)) {
            filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        }
        List<Map<String, String>> list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_NURSE_CONTENT.getCode(),
                patientId, filters, new String[]{"LAST_MODIFY_DATE_TIME", "NR_NAME"});
        //outPatientId
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] outPids = outPatientId.split(",");
            for (String outPid : outPids) {
                String[] pid_vid = outPid.split("\\|");
                if (!patientId.equals(pid_vid[1]) && !visitType.equals(pid_vid[0])) {
                    List<Map<String, String>> list2 = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_NURSE_CONTENT.getCode(),
                            pid_vid[1], filters, new String[]{"LAST_MODIFY_DATE_TIME", "NR_NAME"});
                    list.addAll(list2);
                }
            }
        }
        return list.size();
    }
}

}
