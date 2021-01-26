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

    /**
     *
     * 住院的就诊列表
     * @param patientId 患者id
     * @param visitType 就诊类型，01门诊，02住院
     * @param year 年份
     */
    public void getAllINVisitsForOper(String patientId, String visitType, String year) {


        //根据主索引查询 Solr 处理门诊患者标识和住院患者id不一致情况

       /* 将 patientId, visitType 参数传给下一级 */
         getInfoFromPatient(patientId, visitType);
       /*
        从查询结果中获取 EID ，
        如果 EID不为空，且 EID的值不等于 "EID未知" ，那么 括号1 {
            设置solr的查询条件为： *:*AND EID:（这里填充刚获取的EID值）；
            从mysql的civ_config表中读取 patInfoCollection 配置项，获取solr集合的名称；
            根据查询条件从对应的solr集合中查询字段：
            括号2 {
                "IN_PATIENT_ID", "OUT_PATIENT_ID", "INP_NO", "PERSON_NAME",
                        "SEX_NAME", "DATE_OF_BIRTH", "VISIT_TYPE_CODE"
            } 括号2 ；
            查出的记录中，如果 IN_PATIENT_ID 为空，则设置 IN_PATIENT_ID = OUT_PATIENT_ID

        } 括号1
        */

/*
        循环调用 getVisitsAndAddToVisits 方法，根据前面查出的 IN_PATIENT_ID，查询当前年份往前20年的就诊记录返回给前端
            （以下为调用连接,传入的参数无意义）：*/
            getVisitsAndAddToVisits(null,null);

    }

    /**
     *
     * @param patientId 患者编号
     *  方法描述: 根据患者编号和年份查询就诊列表
     */
    private void getVisitsAndAddToVisits( String patientId, String year) {
      /*
        增设查询条件 ADMISSION_TIME 大于等于 year-01-01 00:00:00;
        增设查询条件 ADMISSION_TIME 小于等于 year-12-31 23:59:59;
        增设查询条件 TRANS_NO 等于 0;
        根据patientId，结合所有查询条件，从 HDR_PAT_ADT 中查出字段：
        {"DEPT_DISCHARGE_FROM_CODE", "DEPT_DISCHARGE_FROM_NAME",
         "ADMISSION_TIME", "DISCHARGE_TIME", "VISIT_ID", "DEPT_ADMISSION_TO_NAME",
         "DEPT_ADMISSION_TO_CODE", "INP_NO", "IN_PATIENT_ID"}
        返回给上一级。
        */
    }


    public void getInfoFromPatient(String patientId, String visitType) {
/*
        如果 visitType 不为空，那么增设查询条件{
            "VISIT_TYPE_CODE" = visitType;
        }

        根据 patientId 和所有的查询条件，从 HDR_PATIENT 表中查出 {"EID", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "INP_NO",
                "OUTP_NO", "IN_PATIENT_ID", "OUT_PATIENT_ID"}字段。
        如果未查任何记录，返回给上一级 STATUS = 0 ；

        如果查出记录，将记录返回给上一级。
*/

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
