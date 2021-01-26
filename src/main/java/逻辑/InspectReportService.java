package 逻辑;

import com.goodwill.core.orm.MatchType;
import com.goodwill.core.orm.Page;
import com.goodwill.core.orm.Page.Sort;
import com.goodwill.core.orm.PropertyFilter;
import com.goodwill.core.utils.ApplicationException;
import com.goodwill.core.utils.DateUtils;
import com.goodwill.hdr.civ.base.dao.HbaseDao;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.enums.HdrTableEnum;
import com.goodwill.hdr.civ.utils.CivUtils;
import com.goodwill.hdr.civ.utils.ColumnUtil;
import com.goodwill.hdr.civ.utils.ListPage;
import com.goodwill.hdr.civ.utils.Utils;
import com.goodwill.hdr.civ.web.service.SpecialtyViewPowerService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InspectReportService implements InspectReportService {

    @Autowired
    private HbaseDao hbaseDao;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SpecialtyViewPowerService specialtyViewPowerService;

    @Override
    public Page<Map<String, String>> getInspectReportList(String patientId, String visitId, String visitType,
                                                          String orderBy, String orderDir, int pageNo, int pageSize) {
        //分页 排序
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //分页
        boolean pageable = true;
        if (pageNo == 0 || pageSize == 0) {
            pageable = false;
        } else {
            page.setPageNo(pageNo);
            page.setPageSize(pageSize);
        }
        //排序
        if (StringUtils.isBlank(orderBy) || StringUtils.isBlank(orderDir)) {
            page.setOrderBy("REPORT_TIME");
            page.setOrderDir("desc");
        } else {
            page.setOrderBy(orderBy);
            page.setOrderDir(orderDir);
        }

        try {
            if ("OUTPV".equalsIgnoreCase(visitType)) {
                page = getOutpvLabReports(page, patientId, visitId, pageable);
            } else if ("INPV".equalsIgnoreCase(visitType)) {
                page = getInpvLabReports(page, patientId, visitId, pageable);
            }
        } catch (Exception e) {
            logger.error("查询Hbase数据库表 HDR_LAB_REPORT 有误！ ", e);
            throw new ApplicationException("查询检验报告表出错！" + e.getCause());
        }
        if (page.getTotalCount() <= 0) {
            return page;
        }
        //字段映射
        List<Map<String, String>> reports = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> report = new HashMap<String, String>();
            ColumnUtil.convertMapping(report, map, new String[]{"LAB_ITEM_NAME", "REPORT_TIME",
                    "REPORT_NO", "HIGH", "LOW"});
            reports.add(report);
        }
        //重置分页
        page.setResult(reports);
        return page;

    }

    /**
     * @param page      分页对象
     * @param patientId 患者编号
     * @param visitId   就诊次
     * @param pageable  是否分页
     * @return 分页对象
     * @Description 方法描述: 某次门诊的检验报告
     */
    private Page<Map<String, String>> getOutpvLabReports(Page<Map<String, String>> page, String patientId,
                                                         String visitId, boolean pageable) {
        List<Map<String, String>> labs = new ArrayList<Map<String, String>>();
        //通过分页优化查询速度：
        int pageSize = page.getPageSize();
        int pageNo = page.getPageNo();
        page.setPageNo(1);
        page.setPageSize(100000);
        //优先 根据vid查询
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if (StringUtils.isNotBlank(visitId)) {
            filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        }
        filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), "01"));
        //分页判断
        if (pageable) {
            String orderBy = page.getOrderBy();
            String orderDir = page.getOrderDir();
            page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_LAB_REPORT.getCode(), patientId, page, filters,
                    "LAB_ITEM_NAME", "REPORT_TIME", "REPORT_NO");
            Utils.sortListByDate(page.getResult(), orderBy, orderDir);
            //分页
            ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(page.getResult(), pageNo, pageSize);
            labs = listPage.getPagedList();
            page.setResult(labs);
            page.setPageNo(pageNo);
            page.setPageSize(pageSize);
        } else {
            labs = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_LAB_REPORT.getCode(), patientId, filters,
                    "LAB_ITEM_NAME", "REPORT_TIME", "REPORT_NO");
            Utils.sortListByDate(labs, page.getOrderBy(), page.getOrderDir()); //排序
            page.setTotalCount(labs.size());
            page.setResult(labs);
        }

        //TODO 若缺失就诊次，可根据CivUtils中的其他条件筛选

        //上述均未查询到结果，则认为未查询到数据，终止执行
        if (page.getTotalCount() <= 0) {
            return page;
        }
        //统计每份检验报告下的异常的检验细项
        for (Map<String, String> lab : page) {
            Map<String, Object> m = getInspectReportYC(patientId, lab.get("REPORT_NO"));
            lab.put("HIGH", m.get("high").toString());
            lab.put("LOW", m.get("low").toString());
        }
        return page;
    }

    /**
     * @param page      分页对象
     * @param patientId 患者编号
     * @param visitId   就诊次
     * @param pageable  是否分页
     * @return 分页对象
     * @Description 方法描述: 某次住院的检验报告
     */
    private Page<Map<String, String>> getInpvLabReports(Page<Map<String, String>> page, String patientId,
                                                        String visitId, boolean pageable) {
        List<Map<String, String>> labs = new ArrayList<Map<String, String>>();
        int pageSize = page.getPageSize();
        int pageNo = page.getPageNo();
        page.setPageNo(1);
        page.setPageSize(100000);
        //优先  根据vid查询
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if (StringUtils.isNotBlank(visitId)) {
            filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        }
        filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), "02"));
        if (pageable) {
            page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_LAB_REPORT.getCode(), patientId, page, filters,
                    "LAB_ITEM_NAME", "REPORT_TIME", "REPORT_NO");
            //分页
            ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(page.getResult(), pageNo, pageSize);
            labs = listPage.getPagedList();
            page.setResult(labs);
            page.setPageSize(pageSize);
            page.setPageNo(pageNo);
        } else {
            labs = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_LAB_REPORT.getCode(), patientId, filters,
                    "LAB_ITEM_NAME", "REPORT_TIME", "REPORT_NO");
            Utils.sortListByDate(labs, page.getOrderBy(), page.getOrderDir()); //排序
            page.setTotalCount(labs.size());
            page.setResult(labs);
        }

        //根据vid未找到，再根据入院出院时间查询
        if (page.getTotalCount() <= 0) {
            page = CivUtils.getInpLabReports(page, patientId, visitId, pageable);
            //分页
            ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(page.getResult(), pageNo, pageSize);
            labs = listPage.getPagedList();
            page.setResult(labs);
            page.setPageSize(pageSize);
            page.setPageNo(pageNo);
        }

        //以上方式均未找到，终止执行
        if (page.getTotalCount() <= 0) {
            return page;
        }

        //统计每份检验报告下的异常检验细项
        for (Map<String, String> lab : page) {
            Map<String, Object> m = getInspectReportYC(patientId, lab.get("REPORT_NO"));
            lab.put("HIGH", m.get("high").toString());
            lab.put("LOW", m.get("low").toString());
        }
        return page;
    }

    /**
     * 某份检验报告的详情  基本信息+检验细项列表
     *
     * @param patientId 患者编号
     * @param reportNo  报告号
     * @param pageNo    页码
     * @param pageSize  分页单位
     * @param show      显示标记   1:显示异常结果
     */
    public void getInspectReportDetails(String patientId, String reportNo,
                                        int pageNo, int pageSize, String show) {

		/*增设查询条件 "REPORT_NO" = reportNo # 前端传入参数# ；
		根据patienId，结合查询条件，从 HDR_LAB_REPORT 表中查出字段
		{"LAB_TYPE_NAME", "SAMPLE_NO", "SAMPLE_TIME", "SPECIMAN_TYPE_NAME",
					"LAB_PERFORMED_TIME", "LAB_ITEM_NAME", "REPORT_TIME"};
		如果未找到报告直接终止。


		读取mysql中civ_config表 CIV_LAB_REPORT_DETAIL_HEAD 配置项，获取检验报告表头配置，返回给前端处理；


		将参数传给 getInspectSubItems 处理获取检验细项列表，并返回给前端；
		*/
        getInspectSubItems(patientId, reportNo, pageNo, pageSize, show);

    }

    /**
     * 某份检验报告的细项列表
     *
     * @param patientId 患者编号
     * @param reportNo  报告号
     * @param pageNo    页码
     * @param pageSize  分页单位
     * @param show      显示标记   1:显示异常结果
     */
    public void getInspectSubItems(String patientId, String reportNo, int pageNo, int pageSize,
                                   String show) {
    /*      如果前端传来的 pageNo等于0 或 pageSize 等于0，表示不可分页；
            增设查询条件 "REPORT_NO" = reportNo ；
    */

   /*       如果可以分页 {
                根据patientId和分页参数，结合查询条件，从 HDR_LAB_REPORT_DETAIL 查询，
                需要查出的字段在mysql的civ_config表 CIV_LAB_REPORT_DETAIL_FIELD 配置；

            }
            如果不可分页 {
                仅根据patientId和分页参数，结合查询条件，从 HDR_LAB_REPORT_DETAIL 查询，
                需要查出的字段在mysql的civ_config表 CIV_LAB_REPORT_DETAIL_FIELD 配置；
            }
    */

    //      未找到细项，终止执行

    //      结果根据"TEST_NO"升序排序

    /*      如果 "LAB_RESULT_VALUE" 不为空，表示是定量结果，那么{
                进行参数映射 "labResultValue"#返回给前端的参数# <----->"LAB_RESULT_VALUE" #HBASE字段#;
                如果 "LAB_RESULT_VALUE" 是数字，那么 "showLine" 设为 true，显示趋势图；
             }
            如果"LAB_RESULT_VALUE" 为空，表示是定性结果，那么进行参数映射 "labResultValue"<----->"LAB_QUAL_RESULT"，
            若"LAB_QUAL_RESULT"为空，则"labResultValue"设为"结果未知"；
    //      异常项判断
            若"RESULT_STATUS_CODE"不为空，那么{
                如果其值为 "H"，映射("status", "high")；
                如果其值为 "L"，映射("status", "low")

            }

             如果 show 等于1，仅保存异常项；
            如果 show 不等于1，保存所有检验项；
            将数据返回给前端。
    */

    }

    public Map<String, Object> getInspectReportYC(String patientId, String reportNo) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            //查询当前检验报告下的  检验细项列表
            List<PropertyFilter> filter2 = new ArrayList<PropertyFilter>();
            filter2.add(new PropertyFilter("REPORT_NO", "STRING", MatchType.EQ.getOperation(), reportNo));
            List<Map<String, String>> subLabs = hbaseDao.findConditionByPatient(
                    HdrTableEnum.HDR_LAB_REPORT_DETAIL.getCode(), patientId, filter2, new String[]{"REPORT_NO",
                            "RESULT_STATUS_CODE"});
            //处理检验细项
            int top = 0;
            int low = 0;
            for (Map<String, String> map : subLabs) {
                //异常项判断
                String status = map.get("RESULT_STATUS_CODE");
                if (StringUtils.isNotBlank(status)) {
                    if ("H".equals(status)) {
                        top++;
                    } else if ("L".equals(status)) {
                        low++;
                    }
                }
            }
            //异常的检验项数量
            result.put("high", top);
            result.put("low", low);
        } catch (Exception e) {
            result.put("high", 0);
            result.put("low", 0);
            e.printStackTrace();
            return result;
        }
        return result;
    }


    public void getAllInspectReportYC(Page<Map<String, String>> page, String patientId, String reportNo) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            //查询当前检验报告下的  检验细项列表
            List<PropertyFilter> filter2 = new ArrayList<PropertyFilter>();
            filter2.add(new PropertyFilter("REPORT_NO", "STRING", MatchType.IN.getOperation(), reportNo));
            List<Map<String, String>> subLabs = hbaseDao.findConditionByPatient(
                    HdrTableEnum.HDR_LAB_REPORT_DETAIL.getCode(), patientId, filter2, new String[]{"REPORT_NO",
                            "RESULT_STATUS_CODE"});
            for (Map<String, String> lab : page) {
                if (StringUtils.isBlank(lab.get("HIGH"))) {
                    lab.put("HIGH", "0");
                }
                if (StringUtils.isBlank(lab.get("LOW"))) {
                    lab.put("LOW", "0");
                }
                //处理检验细项
                for (Map<String, String> map : subLabs) {
                    if (lab.get("REPORT_NO").equals(map.get("REPORT_NO"))) {
                        //异常项判断
                        String status = map.get("RESULT_STATUS_CODE");
                        if (StringUtils.isNotBlank(status)) {
                            if ("H".equals(status)) {
                                int high = Integer.parseInt(lab.get("HIGH")) + 1;
                                lab.put("HIGH", String.valueOf(high));
                            } else if ("L".equals(status)) {
                                int high = Integer.parseInt(lab.get("LOW")) + 1;
                                lab.put("LOW", String.valueOf(high));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getInspectReportDetailsLine(String patientId, String visitType, String outpatientId, String dateType, String startDate,
                                                           String endDate, String subItemCode) {
        //封装数据
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(patientId)) {
            getReportDetailsLine(patientId, visitType, dateType, startDate, endDate, subItemCode, list);
        }
        if (StringUtils.isNotBlank(outpatientId)) {
            String[] pats = outpatientId.split(",");
            for (int i = 0; i < pats.length; i++) {
                if (StringUtils.isNotBlank(pats[i])) {
                    String[] pat = pats[i].split("\\|");
                    getReportDetailsLine(pat[1], pat[0], dateType, startDate, endDate, subItemCode, list);
                }
            }
        }
        //时间 降序
        Utils.sortListByDate(list, "REPORT_TIME", "desc");
        //近3次，近5次，需要分页
        int splitNum = 0;
        if (list.size() == 0) {
            splitNum = 1;
        } else {
            splitNum = list.size();
        }
        ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(list, 1, splitNum);
        if ("3".equals(dateType)) {
            listPage = new ListPage<Map<String, String>>(list, 1, 3);
        } else if ("5".equals(dateType)) {
            listPage = new ListPage<Map<String, String>>(list, 1, 5);
        } else if ("10".equals(dateType)) {
            listPage = new ListPage<Map<String, String>>(list, 1, 10);
        } else if ("50".equals(dateType)) {
            listPage = new ListPage<Map<String, String>>(list, 1, 50);
        }
        List<Map<String, String>> pagedList = listPage.getPagedList();
        //当前页记录再排序，为统计时间坐标准备
        Utils.sortListByDate(pagedList, "REPORT_TIME", "asc");
        //名称
        String subItemName = null;
        //单位
        String subItemUnit = null;
        //参考值
        String subItemRange = null;
        //x坐标
        List<String> categories = new ArrayList<String>();
        //y坐标  上限值+下限值+结果值
        List<Map<String, Object>> series = new ArrayList<Map<String, Object>>();
        //y轴  检验结果值
        List<Double> valueList = new ArrayList<Double>();
        //遍历检验结果
        for (Map<String, String> map : pagedList) {
            if (StringUtils.isBlank(subItemName)) {
                subItemName = map.get("LAB_SUB_ITEM_NAME");
            }
            if (StringUtils.isBlank(subItemUnit)) {
                subItemUnit = map.get("LAB_RESULT_UNIT");
            }
            if (StringUtils.isBlank(subItemRange)) {
                subItemRange = map.get("RANGE");
            }
            //y轴  先取定量结果  若没有值 再取定性结果
            String resultValue = map.get("LAB_RESULT_VALUE");
            if (StringUtils.isNotBlank(resultValue)) {
                if (Utils.isNumber(resultValue)) {
                    valueList.add(Double.parseDouble(resultValue));
                } else {
                    resultValue = map.get("LAB_QUAL_RESULT");
                    if (Utils.isNumber(resultValue)) {
                        valueList.add(Double.parseDouble(resultValue));
                    }
                }
            } else {
                //取定性结果
                resultValue = map.get("LAB_QUAL_RESULT");
                if (Utils.isNumber(resultValue)) {
                    valueList.add(Double.parseDouble(resultValue));
                }
            }
            //x轴
            String dateValue = map.get("REPORT_TIME");
            categories.add(Utils.getDate("yyyy/MM/dd", dateValue));
        }
        //趋势图特殊标识
        Map<String, Object> mk = new HashMap<String, Object>();
        mk.put("enabled", false);
        //上限值 和 下限值
        Map<String, Object> range = CivUtils.parseRange(subItemRange);
        String max = (String) range.get("max");
        String min = (String) range.get("min");
        //上限值
        Map<String, Object> topMap = new HashMap<String, Object>();
        List<Double> topList = new ArrayList<Double>();
        if (StringUtils.isNotBlank(max)) {
            topList.add(Double.parseDouble(max));
        }
        topMap.put("name", "上限");
        topMap.put("data", topList);
        topMap.put("dashStyle", "ShortDot");
        topMap.put("marker", mk);
        series.add(topMap);
        //结果值
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("name", "数值");
        valueMap.put("data", valueList);
        series.add(valueMap);
        //下限值
        Map<String, Object> downMap = new HashMap<String, Object>();
        List<Double> downList = new ArrayList<Double>();
        if (StringUtils.isNotBlank(min)) {
            downList.add(Double.parseDouble(min));
        }
        downMap.put("name", "下限");
        downMap.put("data", downList);
        downMap.put("dashStyle", "ShortDot");
        downMap.put("marker", mk);
        series.add(downMap);
        //检验细项名称
        if (StringUtils.isNotBlank(subItemName)) {
            result.put("name", subItemName);
        } else {
            result.put("name", subItemCode);
        }
        //检验细项单位
        result.put("unit", subItemUnit);
        //y坐标
        result.put("series", series);
        //x坐标
        result.put("categories", categories);
        return result;
    }

    public void getReportDetailsLine(String patientId, String visitType, String dateType, String startDate, String endDate,
                                     String subItemCode, List<Map<String, String>> list) {
        List<Map<String, String>> rs = new ArrayList<Map<String, String>>();
        //过滤条件
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        //细项编码
        filters.add(new PropertyFilter("LAB_SUB_ITEM_CODE", "STRING", MatchType.EQ.getOperation(), subItemCode));
        filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
        //时间条件
        if ("0".equals(dateType)) {
            if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
                filters.add(new PropertyFilter("REPORT_TIME", "STRING", MatchType.GE.getOperation(), startDate));
                filters.add(new PropertyFilter("REPORT_TIME", "STRING", MatchType.LE.getOperation(), endDate));
            }
        }
        rs = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_LAB_REPORT_DETAIL.getCode(),
                patientId, filters, "REPORT_TIME", "LAB_QUAL_RESULT", "LAB_RESULT_UNIT", "LAB_RESULT_VALUE",
                "LAB_SUB_ITEM_NAME", "LAB_SUB_ITEM_CODE", "RANGE");
        if (rs.size() > 0) {
            list.addAll(rs);
        }
    }

    @Override
    public long getInspectCount(String patientId, String visitId, String visitType) {
        //查询检验报告
        Page<Map<String, String>> page = getInspectReportList(patientId, visitId, visitType, "", "", 0, 0);
        long inspectCount = page.getTotalCount();
        if (inspectCount < 0) {
            inspectCount = 0;
        }
        return inspectCount;
    }

    @Override
    public List<Map<String, Object>> getInspectReports(String patientId, String visitType, int pageNo, int pageSize, String orderby,
                                                       String orderdir, String outpatientId, String year) {
        List<Map<String, String>> reports = new ArrayList<Map<String, String>>();
        //获得所有类型
        List<Map<String, String>> types = getReportsTypes(patientId, outpatientId, visitType);

        //获取默认当前年
        if (StringUtils.isBlank(year)) {
            Calendar date = Calendar.getInstance();
            year = String.valueOf((date.get(Calendar.YEAR) + 1));
        }
        int num = 0;
        while (reports.size() == 0) {
            year = (Integer.valueOf(year) - 1) + "";
            if (StringUtils.isNotBlank(patientId)) {
                getInspectReportsByPat(patientId, visitType, types, reports, year);
            }
            if (StringUtils.isNotBlank(outpatientId)) {
                String[] pats = outpatientId.split(",");
                for (int i = 0; i < pats.length; i++) {
                    if (StringUtils.isNotBlank(pats[i])) {
                        String[] pat = pats[i].split("\\|");
                        getInspectReportsByPat(pat[1], pat[0], types, reports, year);
                    }
                }
            }
            num++;
            if (num == 20)
                break;
        }

        //报告信息处理
        Map<String, Object> resultList = new HashMap<String, Object>();
        CivUtils.groupByDate(resultList, reports, "reportTime");
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String time : resultList.keySet()) {
            Map<String, Object> rs = new HashMap<String, Object>();
            rs.put("time", time);
            rs.put("order", Integer.valueOf(CivUtils.changeFormatDate(time)));
            rs.put("data", resultList.get(time));
            result.add(rs);
        }
        Utils.sortListByDate(result, "order", Sort.DESC);
        return result;
    }

    public void getInspectReportsByPat(String patientId, String visitType, List<Map<String, String>> types,
                                       List<Map<String, String>> reports, String year) {
        //分页
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        page.setPageNo(1);
        page.setPageSize(10000);

        //条件过滤  按照检验项查询
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
        if (StringUtils.isNotBlank(year)) {
            PropertyFilter filter1 = new PropertyFilter("REPORT_TIME", "STRING", MatchType.GE.getOperation(), year
                    + "-01-01");
            filters.add(filter1);
            PropertyFilter filter2 = new PropertyFilter("REPORT_TIME", "STRING", MatchType.LE.getOperation(), year
                    + "-12-31 23:59:59");
            filters.add(filter2);
        }

        page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_LAB_REPORT.getCode(), patientId, page, filters);

        //统计每份检验报告下的异常检验细项
        String reportNo = "";
        for (Map<String, String> lab : page) {
            if (lab.get("REPORT_NO") == null) {
                continue;
            }
            if (StringUtils.isBlank(reportNo)) {
                reportNo += lab.get("REPORT_NO");
            } else {
                reportNo += "," + lab.get("REPORT_NO");
            }
//			Map<String, Object> m = getInspectReportYC(patientId, lab.get("REPORT_NO"));
//			lab.put("HIGH", m.get("high").toString());
//			lab.put("LOW", m.get("low").toString());
        }
        //新增统计明细定性结果优化，通过in查询一次统计所有数据再处理，去掉循环查询
        getAllInspectReportYC(page, patientId, reportNo);

        for (Map<String, String> map : page) {
            if (map.get("REPORT_NO") == null) {
                continue;
            }
            Map<String, String> report = new HashMap<String, String>();
            ColumnUtil.convertMapping(report, map, new String[]{"LAB_PERFORMED_TIME", "LAB_ITEM_NAME",
                    "REPORT_TIME", "REPORT_NO", "HIGH", "LOW"});
            for (Map<String, String> type : types) {
                if (map.get("LAB_ITEM_NAME") == null || "".equals(map.get("LAB_ITEM_NAME"))) {
                    if ("其他".equals(type.get("name"))) {
                        report.put("reportTypeCode", type.get("id"));
                    }
                } else {
                    if (map.get("LAB_ITEM_NAME").equals(type.get("name"))) {
                        report.put("reportTypeCode", type.get("id"));
                    }
                }
            }
            report.put("patient_Id", patientId);
            reports.add(report);
        }
    }

    @Override
    public void getAllReportsCount(String patientId, Map<String, Object> resultMap, String outpatientId, String visitType) {

        int num = 0;
        if (StringUtils.isNotBlank(patientId)) {
            num = num + getReportsTypesByPat(patientId, visitType);
        }

        if (StringUtils.isNotBlank(outpatientId)) {
            String[] pats = outpatientId.split(",");
            for (int i = 0; i < pats.length; i++) {
                if (StringUtils.isNotBlank(pats[i])) {
                    String[] pat = pats[i].split("\\|");
                    num = num + getReportsTypesByPat(pat[1], pat[0]);
                }
            }
        }
        resultMap.put("num", num);
    }

    /**
     * @param items     报告类型集合
     * @param patientId 患者编号
     * @param is        标记其他类型
     * @Description 方法描述: 根据患者ID查询报告类型
     */
    public void getReportsTypesByPat(String patientId, String visitType, Map<String, String> items, StringBuffer is) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
        List<Map<String, String>> outpage = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_LAB_REPORT.getCode(),
                patientId, filters, "LAB_ITEM_NAME", "REPORT_NO");
        for (Map<String, String> map : outpage) {
            if (map.get("LAB_ITEM_NAME") == null || "".equals(map.get("LAB_ITEM_NAME"))) {
                is.append("true");
            } else {
                if (null == items.get(map.get("LAB_ITEM_NAME"))) {
                    items.put(map.get("LAB_ITEM_NAME"), map.get("LAB_ITEM_NAME"));
                }
            }
        }
    }

    /**
     * @param patientId 患者编号
     * @return 返回类型：int
     * @Description 方法描述: 根据患者ID查询报告总数
     */
    public int getReportsTypesByPat(String patientId, String visitType) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
        List<Map<String, String>> outpage = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_LAB_REPORT.getCode(),
                patientId, filters, "LAB_ITEM_NAME", "REPORT_NO");
        return outpage.size();
    }

    @Override
    public List<Map<String, String>> getReportsTypes(String patientId, String outpatientId, String visitType) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        Map<String, String> items = new HashMap<String, String>();

        StringBuffer is = new StringBuffer("false");
        if (StringUtils.isNotBlank(patientId)) {
            getReportsTypesByPat(patientId, visitType, items, is);
        }

        if (StringUtils.isNotBlank(outpatientId)) {
            String[] pats = outpatientId.split(",");
            for (int i = 0; i < pats.length; i++) {
                if (StringUtils.isNotBlank(pats[i])) {
                    String[] pat = pats[i].split("\\|");
                    getReportsTypesByPat(pat[1], pat[0], items, is);
                }
            }
        }

        int count = 1;
        for (String item : items.keySet()) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("id", count + "");
            map.put("name", item);
            count++;
            list.add(map);
        }
        if (!"false".equals(is.toString())) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("id", count + "");
            map.put("name", "其他");
            list.add(map);
        }
        return list;
    }

    @Override
    public List<Map<String, String>> getExLabResult(String patientId, String visitId, String visitType, String mainDiag, String deptCode) {
        //异常结果
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
        filters1.add(new PropertyFilter("TRANS_NO", "STRING", MatchType.EQ.getOperation(), "0")); //仅查询入出院记录
        List<Map<String, String>> adts = hbaseDao.findConditionByPatientVisitId(HdrTableEnum.HDR_PAT_ADT.getCode(),
                patientId, visitId, filters1, "VISIT_ID", "ADMISSION_TIME", "DISCHARGE_TIME", "TRANS_NO");
        //未找到住院记录  则认为未查到检验明细
        if (adts.size() == 0) {
            return result;
        }
        Map<String, String> adt = adts.get(0);
        String startTime = adt.get("ADMISSION_TIME");
        if (StringUtils.isBlank(startTime)) {
            return result;
        }
        String endTime = adt.get("DISCHARGE_TIME");
        if (StringUtils.isBlank(endTime)) {
            endTime = DateUtils.getNowDateTime();
        }
        List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
        filters2.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), "02"));
        filters2.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));

        //filters2.add(new PropertyFilter("REPORT_TIME", "STRING", MatchType.GE.getOperation(), startTime));
        //filters2.add(new PropertyFilter("REPORT_TIME", "STRING", MatchType.LT.getOperation(), endTime));

        List<Map<String, String>> labs = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_LAB_REPORT_DETAIL.getCode(),
                patientId, filters2, "LAB_SUB_ITEM_CODE", "LAB_SUB_ITEM_NAME", "RANGE", "LAB_RESULT_UNIT",
                "LAB_RESULT_VALUE", "LAB_QUAL_RESULT", "RESULT_STATUS_CODE");

        //未找到细项，终止执行
        if (labs.size() == 0) {
            return result;
        }
        //处理检验细项
        for (Map<String, String> map : labs) {
            Map<String, String> item = new HashMap<String, String>();
            //字段映射
            ColumnUtil.convertMapping(item, map, new String[]{"LAB_SUB_ITEM_CODE", "LAB_SUB_ITEM_NAME", "RANGE",
                    "LAB_RESULT_UNIT"});
            //检验结果
            if (StringUtils.isNotBlank(map.get("LAB_RESULT_VALUE"))) {
                String labResult = map.get("LAB_RESULT_VALUE"); //定量
                item.put("labResultValue", labResult);
                if (Utils.isNumber(labResult)) {
                    item.put("showLine", "true"); //定量结果存在且为数字，显示趋势图
                }
            } else {
                Utils.checkAndPutToMap(item, "labResultValue", map.get("LAB_QUAL_RESULT"), "结果未知", false); //定性
            }
            //异常项判断
            boolean ex = false;
            String status = map.get("RESULT_STATUS_CODE");
            if (StringUtils.isNotBlank(status)) {
                if ("H".equals(status)) {
                    item.put("status", "high");
                    ex = true;
                } else if ("L".equals(status)) {
                    item.put("status", "low");
                    ex = true;
                }
            }
            //仅保存异常项
            if (ex) {
                result.add(item);
            }
        }
        //查询专科设置并处理结果数据
        List<Map<String, String>> resultData = new ArrayList<Map<String, String>>();
        List<Map<String, String>> resultTemp = new ArrayList<Map<String, String>>();

        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(mainDiag) || StringUtils.isNotBlank(deptCode)) {
            list = specialtyViewPowerService.getSpecialtyConfig(mainDiag, "LabDetail", deptCode);
        }
        //将重点指标筛选出来
        for (Map<String, String> deptMap : list) {
            String sub_item_code_dept = deptMap.get("subItemCode");
            for (Map<String, String> map : result) {
                String sub_item_code = map.get("labSubItemCode");
                if (StringUtils.isNotBlank(sub_item_code) && sub_item_code.equals(sub_item_code_dept)) {
                    resultData.add(map);
                    break;
                }
            }
        }
        //统计剩下的非重点值标
        for (Map<String, String> map : result) {
            String sub_item_code = map.get("labSubItemCode");
            boolean isContinue = false;
            for (Map<String, String> deptMap : resultData) {
                String sub_item_code_dept = deptMap.get("labSubItemCode");
                if (StringUtils.isNotBlank(sub_item_code) && sub_item_code.equals(sub_item_code_dept)) {
                    isContinue = true;
                    break;
                }
            }
            if (isContinue) {
                continue;
            }
            resultTemp.add(map);
        }

        //合并
        for (Map<String, String> map : resultTemp) {
            resultData.add(map);
        }
        return resultData;
    }


    public List<Map<String, String>> getLabReportDetail(String patientId, String visitId, String orderNo, String orderItemCode) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        filters.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
        filters.add(new PropertyFilter("ORDER_ITEM_CODE", "STRING", MatchType.EQ.getOperation(), orderItemCode));
        List<Map<String, String>> outpage = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_LAB_REPORT_DETAIL.getCode(),
                patientId, filters, "ROWKEY", "REPORT_TIME");
        return outpage;
    }

    public List<Map<String, String>> getExamReportDetail(String patientId, String visitId, String orderNo, String orderItemCode) {
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
        filters.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), orderNo));
        List<Map<String, String>> outpage = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(),
                patientId, filters, "ROWKEY", "REPORT_TIME");
        return outpage;
    }
}
