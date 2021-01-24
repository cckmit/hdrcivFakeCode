package 逻辑;


import java.util.*;


public class OrderService {


    public Page<Map<String, String>> getOrderList(String patId, String visitId, String visitType,
                                                  List<String> orderClass, String filterString, String orderReporte, String orderby, String orderdir,
                                                  String mainDiag, String orderCode, String deptCode, int pageNo, int pageSize) {
        //查询患者本次就诊的医嘱，医嘱性质包含长期和临时，医嘱状态包含下达/审核/开始/停止，将状态为撤销的医嘱排除

        //判断就诊类型
        String tableName = null;
        if ("OUTPV".equals(visitType)) { //门诊
            tableName = HdrTableEnum.HDR_OUT_ORDER.getCode();
        } else if ("INPV".equals(visitType)) { //住院
            tableName = HdrTableEnum.HDR_IN_ORDER.getCode();
        }

        //查询条件
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        //排除状态为撤销的医嘱
        //createPropertyFilter("ORDER_STATUS_NAME", "撤销,废弃", MatchType.NOTIN.getOperation(), filters);

        //医嘱类别   药品，检查，检验...
        if (orderClass != null && orderClass.size() > 0) {
            String orderClassString = "";
            for (String orderString : orderClass) {
                orderClassString += orderString + ",";
            }
            orderClassString = orderClassString.substring(0, orderClassString.length() - 1);
            createPropertyFilter("ORDER_CLASS_CODE", orderClassString, MatchType.IN.getOperation(), filters);
        }
        //医嘱性质 和 医嘱状态 或其他条件
        strToFilter(filterString, filters, ";");
        //获取列包括 医嘱号、父医嘱号、医嘱类别、医嘱性质、医嘱项、开始时间、结束时间、医嘱状态、开据医生
        //读取出医嘱列表，分页查询  按医嘱时间倒序排列
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //分页判断
        boolean pageable = true;
        if (pageNo == 0 || pageSize == 0) {
            pageable = false;
        } else {
            page.setPageNo(1);
            page.setPageSize(100000);
        }
        //排序
        if (StringUtils.isBlank(orderby) || StringUtils.isBlank(orderdir)) {
            page.setOrderBy("ORDER_TIME");
            page.setOrderDir("desc");
        } else {
            page.setOrderBy(orderby);
            page.setOrderDir(orderdir);
        }
        String[] columnsStrs = new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME", "DOSAGE_VALUE", "TOTAL_DOSAGE_VALUE",
                "DOSAGE_UNIT", "PHARMACY_WAY_NAME", "FREQUENCY_NAME", "ORDER_PROPERTIES_NAME",
                "ORDER_DOCTOR_NAME", "ORDER_TIME", "ORDER_BEGIN_TIME", "ORDER_END_TIME",
                "ORDER_STATUS_NAME", "PARENT_ORDER_NO", "ORDER_NO", "ORDER_ITEM_CODE", "APPLY_NO",
                "EXECSQN", "BRAND", "SPEED_RATE_VALUE", "SPEED_RATE_UNIT", "SPECIFICATION"};
        List<String> columnsList = new ArrayList<>(Arrays.asList(columnsStrs));
        //获取配置的字段
        String columnConf = Config.getConfigValue("ORDER_SELECT_COLUMNS");
        if (StringUtils.isNotBlank(columnConf)) {
            String[] columnConfs = columnConf.split(",");
            columnsList.addAll(Arrays.asList(columnConfs));
        }
        String[] columns = columnsList.toArray(new String[]{});
        if (pageable) {
            page = hbaseDao.findPageConditionByPatientVisitId(tableName, patId, visitId, page, filters,
                    columns);
        } else {
            List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(tableName, patId, visitId, filters,
                    columns);
            //排序
            Utils.sortListByDate(list, page.getOrderBy(), page.getOrderDir());
            page.setTotalCount(list.size());
            //增加设置数据集代码-pjp
            page.setResult(list);

            if (StringUtils.isNotBlank(mainDiag) || StringUtils.isNotBlank(orderCode)) {
                //查询专科视图配置  用药医嘱
                List<Map<String, String>> listDept = specialtyViewPowerService.getSpecialtyConfig(mainDiag, orderCode,
                        deptCode);
                List<Map<String, String>> data = new ArrayList<Map<String, String>>();
                List<Map<String, String>> dataTemp = new ArrayList<Map<String, String>>();
                for (Map<String, String> map : page) {
                    String orderCodeTemp = map.get("orderItemCode");
                    map.remove("orderItemCode");
                    boolean isBreak = false;
                    for (Map<String, String> mapSpecialty : listDept) {
                        String code = mapSpecialty.get("subItemCode");
                        if (StringUtils.isNotBlank(orderCodeTemp) && orderCodeTemp.equals(code)) {
                            data.add(map);
                            isBreak = true;
                            break;
                        }
                    }
                    if (isBreak) {
                        continue;
                    }
                    dataTemp.add(map);
                }
                //合并数据
                for (Map<String, String> map : dataTemp) {
                    data.add(map);
                }
                page.setResult(data);
            }

        }
        //循环该页医嘱的每一条数据，根据医嘱类型，单独从医嘱执行表和报告表中读取执行数据和报告数据
        for (Map<String, String> map : page) {
            //拼接医嘱排序字段
            if (StringUtils.isBlank(map.get("PARENT_ORDER_NO"))) {
                map.put("PARENT_ORDER_NO", map.get("ORDER_NO"));
            }
            map.put("GROUPSORTCOLUMN", map.get("ORDER_TIME") + map.get("PARENT_ORDER_NO"));

            if (StringUtils.isNotBlank(orderReporte)) {
                String column = Config.getCIV_ORDER_LABOREXAM();
                String no = map.get(column);
                //检验医嘱特殊处理
                if ("lab".equals(orderReporte)) {
                    getLabReportStatus(map, patId);
                }
                //检查医嘱特殊处理
                if ("exam".equals(orderReporte)) {
                    map.put("REPORT_STATUS", "报告未出");
                    if (StringUtils.isBlank(no)) {
                        continue;
                    }
                    filters = new ArrayList<PropertyFilter>();
                    String operation = Config.getCIV_ORDER_EXAM_MATCHTYPE();
                    createPropertyFilter(column, no, operation, filters);
                    List<Map<String, String>> labReport = hbaseDao.findConditionByPatient(
                            HdrTableEnum.HDR_EXAM_REPORT.getCode(), patId, filters,
                            new String[]{"REPORT_DOCTOR_NAME", "REPORT_DOCTOR_CODE", "REPORT_TIME"});
                    if (labReport.size() > 0) {
                        if (StringUtils.isNotBlank(labReport.get(0).get("REPORT_DOCTOR_NAME"))
                                || StringUtils.isNotBlank(labReport.get(0).get("REPORT_DOCTOR_CODE"))
                                || StringUtils.isNotBlank(labReport.get(0).get("REPORT_TIME"))) {
                            map.put("REPORT_STATUS", "报告已出");
                        }
                    }
                }
            } else {
                //如果为空则直接跳出循环
                continue;
            }
        }
        return page;
    }

    @Override
    public Page<Map<String, String>> getOperOrderList(String patId, String visitId, String visitType,
                                                      List<String> types, String orderNo, String orderBy, String orderDir, int pageNo, int pageSize) {
        //将患者的全部医嘱取出，医嘱性质包含长期和临时，医嘱状态包含下达/审核/开始/停止，将状态为撤销的医嘱排除
        String tableName = "HDR_IN_ORDER";
        if ("OUTPV".equals(visitType)) {
            tableName = "HDR_OUT_ORDER";
        }
        //页面查询条件
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        //		createPropertyFilter("ORDER_STATUS_NAME", "撤销,废弃", MatchType.NOTIN.getOperation(), filters);
        if (StringUtils.isNotBlank(orderNo)) {
            createPropertyFilter("ORDER_NO", orderNo, MatchType.EQ.getOperation(), filters);
        }

        if (types != null && types.size() > 0) {
            String orderClassString = "";
            for (String orderString : types) {
                orderClassString += orderString + ",";
            }
            orderClassString = orderClassString.substring(0, orderClassString.length() - 1);
            createPropertyFilter("ORDER_CLASS_CODE", orderClassString, MatchType.IN.getOperation(), filters);
        }
        //这里是获取手术配置的条件
        Config.setVisitViewOperationFilter(filters);
        //获取列包括 医嘱号、父医嘱号、医嘱类别、医嘱性质、医嘱项、开始时间、结束时间、医嘱状态、开据医生
        //读取出医嘱列表，并按医嘱时间倒序
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (pageSize == 0) {
            pageSize = 30;
        }
        page.setOrderBy(orderBy);
        page.setOrderDir(orderDir);
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        page = hbaseDao.findPageConditionByPatientVisitId(tableName, patId, visitId, page, filters,
                new String[]{"ORDER_NO", "PARENT_ORDER_NO", "ORDER_CLASS_CODE", "ORDER_CLASS_NAME",
                        "ORDER_PROPERTIES_NAME", "ORDER_ITEM_CODE", "ORDER_ITEM_NAME", "ORDER_BEGIN_TIME",
                        "ORDER_END_TIME", "ORDER_STATUS_NAME", "ORDER_DOCTOR_CODE", "ORDER_DOCTOR_NAME", "ORDER_TIME",
                        "FREQUENCY_NAME", "DOSAGE_VALUE", "DOSAGE_UNIT", "PHARMACY_WAY_NAME", "ORDER_TEXT"});
        for (Map<String, String> map : page) {
            String orderNO = map.get("ORDER_NO");
            //拼接手术申请数据
            filters = new ArrayList<PropertyFilter>();
            createPropertyFilter("OPER_APPLY_NO", orderNO, MatchType.EQ.getOperation(), filters);
            List<Map<String, String>> operApplyList = hbaseDao.findConditionByPatient("HDR_OPER_APPLY",
                    patId, filters,
                    new String[]{"OPERATION_NAME", "DIAG_BEFORE_OPERATION_NAME", "PLAN_OPER_DOCTOR_NAME",
                            "PLAN_OPER_TIME", "APPLY_OPER_TIME"});
            if (operApplyList.size() > 0) {
                map.put("ORDER_ITEM_NAME",
                        Utils.objToStr(operApplyList.get(0).get("OPERATION_NAME"), map.get("ORDER_ITEM_NAME")));
                map.put("DIAG_BEFORE_OPERATION_NAME",
                        Utils.objToStr(operApplyList.get(0).get("DIAG_BEFORE_OPERATION_NAME")));
                map.put("PLAN_OPER_DOCTOR_NAME", Utils.objToStr(operApplyList.get(0).get("PLAN_OPER_DOCTOR_NAME")));
                map.put("PLAN_OPER_TIME", Utils.objToStr(operApplyList.get(0).get("PLAN_OPER_TIME"))); //拟手术日期
                map.put("APPLY_OPER_TIME", Utils.objToStr(operApplyList.get(0).get("APPLY_OPER_TIME"))); //申请手术时间
            }

            //拼接手术过程数据
            filters = new ArrayList<PropertyFilter>();
            createPropertyFilter("ORDER_NO", orderNO, MatchType.EQ.getOperation(), filters);
            List<Map<String, String>> operAnaesList = hbaseDao.findConditionByKey("HDR_OPER_ANAES",
                    patId, filters,
                    new String[]{"ANESTHESIA_END_TIME", "ANESTHESIA_DOCTOR_NAME"});
            if (operAnaesList.size() > 0) {
                map.put("ANESTHESIA_END_TIME", Utils.objToStr(operAnaesList.get(0).get("ANESTHESIA_END_TIME")));
                map.put("ANESTHESIA_DOCTOR_NAME", Utils.objToStr(operAnaesList.get(0).get("ANESTHESIA_DOCTOR_NAME")));
            }

        }
        return page;
    }

    @Override
    public Page<Map<String, String>> getOrderListMZ(String patId, String visitId, List<String> types, String filterStr,
                                                    String orderReport, String orderBy, String orderDir, int pageNo, int pageSize) {
        String tableName = "HDR_OUT_CHARGE";
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        //医嘱类别   药品，检查，检验...
        if (types != null && types.size() > 0) {
            String orderClassString = "";
            for (String orderString : types) {
                orderClassString += orderString + ",";
            }
            orderClassString = orderClassString.substring(0, orderClassString.length() - 1);
            createPropertyFilter("ORDER_CLASS_CODE", orderClassString, MatchType.IN.getOperation(), filters);
        }

        //医嘱性质 和 医嘱状态 或其他条件
        strToFilter(filterStr, filters, ";");
        //医嘱查询
        List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(tableName, patId, visitId, filters,
                new String[]{"ORDER_NO", "CHARGE_NAME", "TOTAL_DOSAGE_VALUE", "TOTAL_DOSAGE_UNIT",
                        "PHARMACY_WAY_NAME", "FREQUENCY_NAME", "ORDER_CLASS_NAME", "ORDER_ITEM_NAME",
                        "ORDER_DOCTOR_NAME", "ORDER_TIME", "PRESC_TIME", "CHARGE_TIME", "CHARGE_CLASS_NAME",
                        "BILL_ITEM_NAME", "PRES_STATUS_NAME", "PARENT_ORDER_NO", "APPLY_NO"});
        for (Map<String, String> map : list) {
            if (StringUtils.isNotBlank(orderReport)) {
                String column = Config.getCIV_ORDER_LABOREXAM();
                String no = map.get(column);
                //检验医嘱特殊处理
                if ("lab".equals(orderReport)) {
                    getLabReportStatus(map, patId);
                }
                //检查医嘱特殊处理
                if ("exam".equals(orderReport)) {
                    map.put("REPORT_STATUS", "报告未出");
                    //重置过滤条件
                    filters = new ArrayList<PropertyFilter>();
                    createPropertyFilter(column, no, MatchType.EQ.getOperation(), filters);
                    List<Map<String, String>> labReport = hbaseDao.findConditionByPatient(
                            HdrTableEnum.HDR_EXAM_REPORT.getCode(), patId, filters,
                            new String[]{"REPORT_DOCTOR_NAME", "REPORT_DOCTOR_CODE", "REPORT_TIME"});
                    if (labReport.size() > 0) {
                        if (StringUtils.isNotBlank(labReport.get(0).get("REPORT_DOCTOR_NAME"))
                                || StringUtils.isNotBlank(labReport.get(0).get("REPORT_DOCTOR_CODE"))
                                || StringUtils.isNotBlank(labReport.get(0).get("REPORT_TIME"))) {
                            map.put("REPORT_STATUS", "报告已出");
                        }
                    }
                }
            } else {
                //为空，中断循环，跳出
                break;
            }
        }
        //排序
        if (StringUtils.isNotBlank(orderBy) && StringUtils.isNotBlank(orderDir)) {
            Utils.sortListMulti(list, new String[]{orderBy}, new String[]{orderDir});
        } else {
            Utils.sortListByDate(list, "ORDER_TIME", "desc");
        }
        //分页判断
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (pageSize == 0) { //不分页
            page.setResult(list);
            page.setTotalCount(list.size());
        } else { //分页
            ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(list, pageNo, pageSize);
            page.setPageNo(pageNo);
            page.setPageSize(pageSize);
            page.setResult(listPage.getPagedList());
            page.setTotalCount(listPage.getTotalCount());
        }
        return page;
    }

    @Override
    public Page<Map<String, String>> getOperOrderListMZ(String patId, String visitId, List<String> types,
                                                        String filterStr, String orderBy, String orderDir, int pageNo, int pageSize) {
        Page<Map<String, String>> page = getOrderListMZ(patId, visitId, types, filterStr, "", orderBy, orderDir, pageNo,
                pageSize);
        //处理手术医嘱  补全数据
        for (Map<String, String> map : page) {
            String orderNO = map.get("ORDER_NO");
            //拼接手术申请数据
            List<PropertyFilter> filters1 = new ArrayList<PropertyFilter>();
            createPropertyFilter("OPER_APPLY_NO", orderNO, MatchType.EQ.getOperation(), filters1);
            List<Map<String, String>> operApplyList = hbaseDao.findConditionByKey("HDR_OPER_APPLY",
                    patId, filters1,
                    new String[]{"OPERATION_NAME", "DIAG_BEFORE_OPERATION_NAME", "PLAN_OPER_DOCTOR_NAME",
                            "PLAN_OPER_TIME", "APPLY_OPER_TIME"});
            if (operApplyList.size() > 0) {
                map.put("ORDER_ITEM_NAME",
                        Utils.objToStr(operApplyList.get(0).get("OPERATION_NAME"), map.get("ORDER_ITEM_NAME")));
                map.put("DIAG_BEFORE_OPERATION_NAME",
                        Utils.objToStr(operApplyList.get(0).get("DIAG_BEFORE_OPERATION_NAME")));
                map.put("PLAN_OPER_DOCTOR_NAME", Utils.objToStr(operApplyList.get(0).get("PLAN_OPER_DOCTOR_NAME")));
                map.put("PLAN_OPER_TIME", Utils.objToStr(operApplyList.get(0).get("PLAN_OPER_TIME"))); //拟手术日期
                map.put("APPLY_OPER_TIME", Utils.objToStr(operApplyList.get(0).get("APPLY_OPER_TIME"))); //申请手术时间
            }

            //拼接手术过程数据
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            createPropertyFilter("ORDER_NO", orderNO, MatchType.EQ.getOperation(), filters2);
            List<Map<String, String>> operAnaesList = hbaseDao.findConditionByKey("HDR_OPER_ANAES",
                    patId, filters2,
                    new String[]{"ANESTHESIA_END_TIME", "ANESTHESIA_DOCTOR_NAME"});
            if (operAnaesList.size() > 0) {
                map.put("ANESTHESIA_END_TIME", Utils.objToStr(operAnaesList.get(0).get("ANESTHESIA_END_TIME")));
                map.put("ANESTHESIA_DOCTOR_NAME", Utils.objToStr(operAnaesList.get(0).get("ANESTHESIA_DOCTOR_NAME")));
            }

        }
        return page;
    }

    @Override
    public Page<Map<String, String>> getBloodApplyList(String patId, String visitId, String orderBy, String orderDir,
                                                       int pageNo, int pageSize) {
        String tableName = "HDR_BLOOD_APPLY";
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        //输血闭环配置存储到配置文件
        String bloodfilter = Config.getOCLBloodListFilter();
        strToFilter(bloodfilter, filters, ";");
        //createPropertyFilter("TEMPLET_ID", "EMR34.00.01_2", MatchType.EQ.getOperation(), filters);
        //分页
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (pageSize == 0) {
            pageSize = 30;
        }
        page.setOrderBy(orderBy);
        page.setOrderDir(orderDir);
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        page = hbaseDao.findPageConditionByPatientVisitId(tableName, patId, visitId, page, filters,
                new String[]{"ORDER_NO", "APPLY_DEPT_NAME", "TIMES_NO", "TEMPLET_NAME", "APPLY_PERSON_NAME",
                        "APPLY_DATE", "TEMPLET_STATE_NAME", "APPLY_NO"});
        //增加申请单状态
        for (Map<String, String> map : page) {
            List<PropertyFilter> filters2 = new ArrayList<PropertyFilter>();
            createPropertyFilter("APPLY_NO", map.get("APPLY_NO"), MatchType.EQ.getOperation(), filters2);
            List<Map<String, String>> list2 = hbaseDao.findConditionByPatientVisitId("HDR_BLOOD_BANK", patId, visitId,
                    filters2, new String[]{"SQD_STATUS_NAME"});
            if (list2.size() > 0) {
                map.put("SQD_STATUS_NAME", Utils.objToStr(list2.get(0).get("SQD_STATUS_NAME")));
            } else {
                map.put("SQD_STATUS_NAME", map.get("TEMPLET_STATE_NAME"));
            }
        }
        return page;
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
     * 获取检验医嘱的报告状态
     *
     * @param patId 患者号
     * @param map   检验医嘱MAP
     */
    public void getLabReportStatus(Map<String, String> map, String patId) {
        map.put("REPORT_STATUS", "报告未出");
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        String operation = Config.getCIV_ORDER_EXAM_MATCHTYPE();
        String column = Config.getCIV_ORDER_LABOREXAM();
        String no = map.get(column);
        if (StringUtils.isBlank(no)) {
            return;
        }
        createPropertyFilter(column, no, operation, filters);
        List<Map<String, String>> labReport = hbaseDao.findConditionByPatient(
                HdrTableEnum.HDR_LAB_REPORT_DETAIL.getCode(), patId, filters,
                new String[]{"REPORT_NO", "REPORT_TIME"});
        if (labReport.size() > 0) {
            map.put("REPORT_STATUS", "报告已出");
        }
    }

    @Override
    public Page<Map<String, String>> getDrugListKF(String patId, String visitId, String visitType, String orderStatus,
                                                   String orderProperty, String mainDiag, String orderCode, String deptCode, int pageNo, int pageSize) {
        //过滤条件
        StringBuffer filterStr = new StringBuffer();
        //医嘱性质   1-临时    2-长期
        if (StringUtils.isNotBlank(orderProperty)) {
            if ("1".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_SHORT_PROPERTY_CONFIG() + ";");
            } else if ("2".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_LONG_PROPERTY_CONFIG() + ";");
            }
        }
        //医嘱状态
        if (StringUtils.isNotBlank(orderStatus)) {
            filterStr.append("ORDER_STATUS_NAME|like|" + orderStatus + ";");
        }
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //北医三院门诊医嘱特殊处理
        if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache("org_oid")) && "OUTPV".equals(visitType)) {
            //置空条件
            filterStr = new StringBuffer();
            //口服药品
            filterStr.append("PHARMACY_WAY_NAME|like|口服;"); //用药方式
            page = getOrderListMZ(patId, visitId, CivUtils.getOrderClass("BYSY_MZ_DRUG"), filterStr.toString(), "",
                    "ORDER_TIME", "desc", pageNo, pageSize);
            //字段映射
            List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : page) {
                Map<String, String> order = new HashMap<String, String>();
                ColumnUtil.convertMapping(order, map, "ORDER_NO", "ORDER_TIME", "ORDER_DOCTOR_NAME",
                        "ORDER_PROPERTIES_NAME", "FREQUENCY_NAME", "PHARMACY_WAY_NAME", "TOTAL_DOSAGE_VALUE",
                        "ORDER_CLASS_NAME", "ORDER_ITEM_NAME", "ORDER_ITEM_CODE");
                //特殊字段转换
                Utils.checkAndPutToMap(order, "dosageUnit", map.get("TOTAL_DOSAGE_UNIT"), "-", false); //单位
                Utils.checkAndPutToMap(order, "orderStatusName", map.get("PRES_STATUS_NAME"), "-", false); //医嘱状态
                Utils.checkAndPutToMap(order, "orderBeginTime", map.get("CHARGE_TIME"), "-", false); //开始时间
                Utils.checkAndPutToMap(order, "orderEndTime", map.get("PRESC_TIME"), "-", false); //结束时间
                Utils.checkAndPutToMap(order, "orderItemName", map.get("CHARGE_NAME"), "-", false); //医嘱项名称
                orders.add(order);
            }
            //重置分页
            page.setResult(orders);
            return page;
        }
        //非北医三院门诊  正常查询
        //用药方式区分门诊和住院
        String PHARMACY_WAY = null;
        if ("OUTPV".equals(visitType)) {
            PHARMACY_WAY = Config.getOclMzKffilter();
        } else if ("INPV".equals(visitType)) {
            PHARMACY_WAY = Config.getOCLKFFilter();
        }
        if (StringUtils.isNotBlank(PHARMACY_WAY)) {
            filterStr.append(PHARMACY_WAY + ";");
        }
        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_DRUG_KF");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_DRUG_KF");
        }
        page = getOrderList(patId, visitId, visitType, orderClassStrings, filterStr.toString(), "", "ORDER_TIME",
                "desc", mainDiag, orderCode, deptCode, pageNo, pageSize);
        //字段映射
        //字段映射
        String[] columnsStrs = new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME", "DOSAGE_VALUE", "DOSAGE_UNIT",
                "PHARMACY_WAY_NAME", "FREQUENCY_NAME", "ORDER_PROPERTIES_NAME", "ORDER_DOCTOR_NAME",
                "ORDER_TIME", "ORDER_BEGIN_TIME", "ORDER_END_TIME", "ORDER_STATUS_NAME", "ORDER_NO",
                "ORDER_ITEM_CODE", "BRAND", "SPEED_RATE_VALUE", "SPEED_RATE_UNIT"};
        List<String> columnsList = new ArrayList<>(Arrays.asList(columnsStrs));
        //获取配置的字段
        String columnConf = Config.getConfigValue("ORDER_SELECT_COLUMNS");
        if (StringUtils.isNotBlank(columnConf)) {
            String[] columnConfs = columnConf.split(",");
            columnsList.addAll(Arrays.asList(columnConfs));
        }
        String[] columns = columnsList.toArray(new String[]{});
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            ColumnUtil.convertMapping(order, map, columns);
            orders.add(order);
        }
        //重置分页结果
        page.setResult(orders);
        return page;
    }

    @Override
    public Page<Map<String, String>> getDrugListJMZS(String patId, String visitId, String visitType, String orderStatus,
                                                     String orderProperty, String mainDiag, String orderCode, String deptCode, int pageNo, int pageSize) {
        //过滤条件
        StringBuffer filterStr = new StringBuffer();
        //医嘱性质   1-临时    2-长期
        if (StringUtils.isNotBlank(orderProperty)) {
            if ("1".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_SHORT_PROPERTY_CONFIG() + ";");
            } else if ("2".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_LONG_PROPERTY_CONFIG() + ";");
            }
        }
        //医嘱状态
        if (StringUtils.isNotBlank(orderStatus)) {
            filterStr.append("ORDER_STATUS_NAME|like|" + orderStatus + ";");
        }
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //北医三院门诊医嘱特殊处理
        if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache(("org_oid"))) && "OUTPV".equals(visitType)) {
            //置空条件
            filterStr = new StringBuffer();
            //静脉药品
            filterStr.append("PHARMACY_WAY_NAME|like|静脉;"); //用药方式
            page = getOrderListMZ(patId, visitId, CivUtils.getOrderClass("BYSY_MZ_DRUG"), filterStr.toString(), "",
                    "ORDER_TIME", "desc", pageNo, pageSize);
            //字段映射
            List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : page) {
                Map<String, String> order = new HashMap<String, String>();
                ColumnUtil.convertMapping(order, map, "ORDER_NO", "ORDER_TIME", "ORDER_DOCTOR_NAME",
                        "ORDER_PROPERTIES_NAME", "FREQUENCY_NAME", "PHARMACY_WAY_NAME", "TOTAL_DOSAGE_VALUE",
                        "ORDER_CLASS_NAME", "GROUPSORTCOLUMN", "PARENT_ORDER_NO", "ORDER_ITEM_NAME");
                //特殊字段转换
                Utils.checkAndPutToMap(order, "dosageUnit", map.get("TOTAL_DOSAGE_UNIT"), "-", false); //单位
                Utils.checkAndPutToMap(order, "orderStatusName", map.get("PRES_STATUS_NAME"), "-", false); //医嘱状态
                Utils.checkAndPutToMap(order, "orderBeginTime", map.get("CHARGE_TIME"), "-", false); //开始时间
                Utils.checkAndPutToMap(order, "orderEndTime", map.get("PRESC_TIME"), "-", false); //结束时间
                Utils.checkAndPutToMap(order, "orderItemName", map.get("CHARGE_NAME"), "-", false); //医嘱项名称
                orders.add(order);
            }
            //重置分页
            page.setResult(orders);
            return page;
        }
        //非北医三院门诊  正常查询
        //用药方式
        if (StringUtils.isNotBlank(Config.getOCLJMZSFilter())) {
            if ("OUTPV".equals(visitType)) {
                filterStr.append(Config.getOclMzJmzsfilter() + ";");
            } else if ("INPV".equals(visitType)) {
                filterStr.append(Config.getOCLJMZSFilter() + ";");
            }
        }
        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_DRUG_JMZS");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_DRUG_JMZS");
        }
        page = getOrderList(patId, visitId, visitType, orderClassStrings, filterStr.toString(), "", "ORDER_TIME",
                "desc", mainDiag, orderCode, deptCode, pageNo, pageSize);
        //字段映射
        String[] columnsStrs = new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME", "DOSAGE_VALUE", "DOSAGE_UNIT",
                "PHARMACY_WAY_NAME", "FREQUENCY_NAME", "ORDER_PROPERTIES_NAME", "ORDER_DOCTOR_NAME",
                "ORDER_TIME", "ORDER_BEGIN_TIME", "ORDER_END_TIME", "ORDER_STATUS_NAME", "ORDER_NO",
                "PARENT_ORDER_NO", "GROUPSORTCOLUMN", "ORDER_ITEM_CODE", "BRAND", "SPEED_RATE_VALUE", "SPEED_RATE_UNIT"};
        List<String> columnsList = new ArrayList<>(Arrays.asList(columnsStrs));
        //获取配置的字段
        String columnConf = Config.getConfigValue("ORDER_SELECT_COLUMNS");
        if (StringUtils.isNotBlank(columnConf)) {
            String[] columnConfs = columnConf.split(",");
            columnsList.addAll(Arrays.asList(columnConfs));
        }
        String[] columns = columnsList.toArray(new String[]{});
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            ColumnUtil.convertMapping(order, map, columns);
            orders.add(order);
        }
        //重置分页结果
        page.setResult(orders);
        return page;
    }

    @Override
    public Page<Map<String, String>> getDrugListQTYP(String patId, String visitId, String visitType, String orderStatus,
                                                     String orderProperty, int pageNo, int pageSize) {
        //过滤条件
        StringBuffer filterStr = new StringBuffer();
        //医嘱性质   1-临时    2-长期
        if (StringUtils.isNotBlank(orderProperty)) {
            if ("1".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_SHORT_PROPERTY_CONFIG() + ";");
            } else if ("2".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_LONG_PROPERTY_CONFIG() + ";");
            }
        }
        //医嘱状态
        if (StringUtils.isNotBlank(orderStatus)) {
            filterStr.append("ORDER_STATUS_NAME|like|" + orderStatus + ";");
        }
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //北医三院门诊医嘱特殊处理
        if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache("org_oid")) && "OUTPV".equals(visitType)) {
            //置空条件
            filterStr = new StringBuffer();
            //其他药品
            page = getOrderListMZ(patId, visitId, CivUtils.getOrderClass("OrderClose_MZ_DRUG_QTYP"),
                    filterStr.toString(), "", "ORDER_TIME", "desc", pageNo, pageSize);
            //字段映射
            List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : page) {
                Map<String, String> order = new HashMap<String, String>();
                ColumnUtil.convertMapping(order, map, "ORDER_NO", "ORDER_TIME", "ORDER_DOCTOR_NAME",
                        "ORDER_PROPERTIES_NAME", "FREQUENCY_NAME", "PHARMACY_WAY_NAME", "TOTAL_DOSAGE_VALUE",
                        "ORDER_CLASS_NAME", "ORDER_ITEM_NAME");
                //特殊字段转换
                Utils.checkAndPutToMap(order, "dosageUnit", map.get("TOTAL_DOSAGE_UNIT"), "-", false); //单位
                Utils.checkAndPutToMap(order, "orderStatusName", map.get("PRES_STATUS_NAME"), "-", false); //医嘱状态
                Utils.checkAndPutToMap(order, "orderBeginTime", map.get("CHARGE_TIME"), "-", false); //开始时间
                Utils.checkAndPutToMap(order, "orderEndTime", map.get("PRESC_TIME"), "-", false); //结束时间
                Utils.checkAndPutToMap(order, "orderItemName", map.get("CHARGE_NAME"), "-", false); //医嘱项名称
                orders.add(order);
            }
            //重置分页
            page.setResult(orders);
            return page;
        }
        //非北医三院门诊  正常查询
        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        //用药方式
        if (StringUtils.isNotBlank(Config.getOclMzQTfilter()) || StringUtils.isNotBlank(Config.getOCLQTFilter())) {
            if ("OUTPV".equals(visitType)) {
                filterStr.append(Config.getOclMzQTfilter() + ";");
            } else if ("INPV".equals(visitType)) {
                filterStr.append(Config.getOCLQTFilter() + ";");
            }
        }
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_DRUG_QTYP");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_DRUG_QTYP");
        }
        page = getOrderList(patId, visitId, visitType, orderClassStrings, filterStr.toString(), "", "ORDER_TIME",
                "desc", "", "", "", pageNo, pageSize);
        //字段映射
        String[] columnsStrs = new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME", "DOSAGE_VALUE", "DOSAGE_UNIT",
                "PHARMACY_WAY_NAME", "FREQUENCY_NAME", "ORDER_PROPERTIES_NAME", "ORDER_DOCTOR_NAME",
                "ORDER_TIME", "ORDER_BEGIN_TIME", "ORDER_END_TIME", "ORDER_STATUS_NAME", "ORDER_NO", "BRAND", "SPEED_RATE_VALUE", "SPEED_RATE_UNIT"};
        List<String> columnsList = new ArrayList<>(Arrays.asList(columnsStrs));
        //获取配置的字段
        String columnConf = Config.getConfigValue("ORDER_SELECT_COLUMNS");
        if (StringUtils.isNotBlank(columnConf)) {
            String[] columnConfs = columnConf.split(",");
            columnsList.addAll(Arrays.asList(columnConfs));
        }
        String[] columns = columnsList.toArray(new String[]{});
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            ColumnUtil.convertMapping(order, map, columns);
            orders.add(order);
        }
        //重置分页结果
        page.setResult(orders);
        return page;
    }

    @Override
    public Page<Map<String, String>> getCVDrugList(String patientId, String visitId, String orderType, String visitType,
                                                   String orderStatus, String orderProperty, String mainDiag, String deptCode, String orderCode, int pageNo,
                                                   int pageSize) {
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //类型判断 口服 药品
        if (StringUtils.isBlank(orderType)) {
            List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            //口服 + 静脉
            Page<Map<String, String>> kp = getDrugListKF(patientId, visitId, visitType, orderStatus, orderProperty,
                    mainDiag, orderCode, deptCode, 0, 0);
            Page<Map<String, String>> jp = getDrugListJMZS(patientId, visitId, visitType, orderStatus, orderProperty,
                    mainDiag, orderCode, deptCode, 0, 0);
            //合并
            list.addAll(kp.getResult());
            list.addAll(jp.getResult());
            //排序
            Utils.sortListByDate(list, "orderTime", "desc");
            //按时间排序完后把重要指标显示在前列
            //查询专科视图配置  用药医嘱
            List<Map<String, String>> listDept = specialtyViewPowerService.getSpecialtyConfig(mainDiag, orderCode,
                    deptCode);
            List<Map<String, String>> data = new ArrayList<Map<String, String>>();
            List<Map<String, String>> dataTemp = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : list) {
                String orderCodeTemp = map.get("orderItemCode");
                map.remove("orderItemCode");
                boolean isBreak = false;
                for (Map<String, String> mapSpecialty : listDept) {
                    String code = mapSpecialty.get("subItemCode");
                    if (StringUtils.isNotBlank(orderCodeTemp) && orderCodeTemp.equals(code)) {
                        data.add(map);
                        isBreak = true;
                        break;
                    }
                }
                if (isBreak) {
                    continue;
                }
                dataTemp.add(map);
            }
            //合并数据
            for (Map<String, String> map : dataTemp) {
                data.add(map);
            }

            //分页
            ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(data, pageNo, pageSize);
            page.setPageNo(listPage.getNowPage());
            page.setPageSize(listPage.getPageSize());
            page.setResult(listPage.getPagedList());
            page.setTotalCount(listPage.getTotalCount());
        } else if ("kf".equals(orderType)) {
            page = getDrugListKF(patientId, visitId, visitType, orderStatus, orderProperty, mainDiag, orderCode,
                    deptCode, pageNo, pageSize);
        } else if ("jm".equals(orderType)) {
            page = getDrugListJMZS(patientId, visitId, visitType, orderStatus, orderProperty, mainDiag, orderCode,
                    deptCode, pageNo, pageSize);
        }

        if (page.getTotalCount() <= 0) {
            return page;
        }
        //		//===========药品不良反应处理==========
        //		if(Config.getCiv_Cv_Drugadr()){
        //			String vtype = "02";
        //			if ("OUTPV".equals(visitType)) {
        //				vtype = "01";
        //			}
        //			for (Map<String, String> map : page) {
        //				//医嘱号
        //				String orderNo = map.get("orderNo");
        //				List<Map<String, String>> mnList = adrMnDao.getDrugMonitorByPv(patientId, visitId, vtype, orderNo);
        //				if (mnList.size() > 0) {
        //					Utils.checkAndPutToMap(map, "reaction", mnList.get(0).get("event_name"), "不良反应情况不明", false);
        //				}
        //			}
        //		}
        return page;
    }

    @Override
    public Page<Map<String, String>> getLabList(String patId, String visitId, String visitType, String mainDiag,
                                                String orderCode, String deptCode, int pageNo, int pageSize) {
        //过滤条件
        StringBuffer filterStr = new StringBuffer();
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //北医三院门诊医嘱特殊处理
        if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache("org_oid")) && "OUTPV".equals(visitType)) {
            //检验医嘱
            filterStr.append("ORDER_CLASS_NAME|like|验;");
            page = getOrderListMZ(patId, visitId, null, filterStr.toString(), "lab", "ORDER_TIME", "desc", pageNo,
                    pageSize);
            //字段映射
            List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : page) {
                Map<String, String> order = new HashMap<String, String>();
                ColumnUtil.convertMapping(order, map, "ORDER_NO", "ORDER_TIME", "ORDER_DOCTOR_NAME", "ORDER_CLASS_NAME",
                        "REPORT_STATUS", "ORDER_ITEM_NAME");
                //特殊字段转换
                Utils.checkAndPutToMap(order, "orderItemName", map.get("CHARGE_NAME"), "-", false); //医嘱项名称
                orders.add(order);
            }
            //重置分页
            page.setResult(orders);
            return page;
        }
        //非北医三院门诊  正常查询
        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_LAB");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_LAB");
        }
        page = getOrderList(patId, visitId, visitType, orderClassStrings, "", "lab", "ORDER_TIME", "desc", mainDiag,
                orderCode, deptCode, pageNo, pageSize);
        //字段映射
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        //处理超时信息
        boolean isOverTimeConfig = Config.getLAB_OVER_TIME();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            boolean isOverTime = false;
            if (isOverTimeConfig) {
                isOverTime = isReportOverTime(patId, visitId, map.get("ORDER_NO"), map.get("ORDER_ITEM_CODE"), "LAB", map.get("ORDER_TIME"));
                order.put("overTime", true == isOverTime ? "超时" : "正常");
            }
            ColumnUtil.convertMapping(order, map, new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME",
                    "ORDER_DOCTOR_NAME", "ORDER_TIME", "ORDER_NO", "REPORT_STATUS", "APPLY_NO", "EXECSQN"});
            orders.add(order);
        }
        //重置分页结果
        page.setResult(orders);
        return page;
    }

    /**
     * 是否超时
     *
     * @return
     */
    public boolean isReportOverTime(String pid, String vid, String orderNo, String orderItemCode, String reportType, String orderTime) {
        List<CmrReportTimeDuration> overTimeHour = visitReportsOverTimeDao.getOverTimeHour(orderItemCode, reportType);
        CmrReportTimeDuration duration = null;
        if (null == overTimeHour || overTimeHour.size() == 0) {
            return false; //未超时
        }
        duration = overTimeHour.get(0);
        int hour = duration.getHour_count();
        //查询报告时间
        List<Map<String, String>> reportDetail = new ArrayList<Map<String, String>>();
        if ("LAB".equalsIgnoreCase(reportType)) {
            reportDetail = inspectReportService.getLabReportDetail(pid, vid, orderNo, orderItemCode);
        } else if (("EXAM".equalsIgnoreCase(reportType))) {
            reportDetail = inspectReportService.getExamReportDetail(pid, vid, orderNo, orderItemCode);
        }
        String reportTime = null;
        if (null != reportDetail && reportDetail.size() > 0) {
            reportTime = reportDetail.get(0).get("REPORT_TIME");
        }
        if (StringUtils.isBlank(reportTime)) {
            reportTime = DateUtils.getNowDateTime();
        }
        int intervalHours = DateUtils.getIntervalHours(reportTime, orderTime);
        if (intervalHours > hour) {
            return true;
        }
        return false;
    }

    @Override
    public Page<Map<String, String>> getExamList(String patId, String visitId, String visitType, String mainDiag,
                                                 String orderCode, String deptCode, int pageNo, int pageSize) {
        //过滤条件
        StringBuffer filterStr = new StringBuffer();
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //北医三院门诊医嘱特殊处理
        if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache("org_oid")) && "OUTPV".equals(visitType)) {
            //检查医嘱
            filterStr.append("ORDER_CLASS_NAME|like|查;");
            page = getOrderListMZ(patId, visitId, null, filterStr.toString(), "exam", "ORDER_TIME", "desc", pageNo,
                    pageSize);
            //字段映射
            List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : page) {
                Map<String, String> order = new HashMap<String, String>();
                ColumnUtil.convertMapping(order, map, "ORDER_NO", "ORDER_TIME", "ORDER_DOCTOR_NAME", "ORDER_CLASS_NAME",
                        "REPORT_STATUS", "ORDER_ITEM_NAME");
                //特殊字段转换
                Utils.checkAndPutToMap(order, "orderItemName", map.get("CHARGE_NAME"), "-", false); //医嘱项名称
                orders.add(order);
            }
            //重置分页
            page.setResult(orders);
            return page;
        }
        //非北医三院门诊 正常查询
        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_EXAM");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_EXAM");
        }
        page = getOrderList(patId, visitId, visitType, orderClassStrings, "", "exam", "ORDER_TIME", "desc", mainDiag,
                orderCode, deptCode, pageNo, pageSize);
        //字段映射
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        //处理超时信息
        boolean isOverTimeConfig = Config.getEXAM_OVER_TIME();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            boolean isOverTime = false;
            if (isOverTimeConfig) {
                isOverTime = isReportOverTime(patId, visitId, map.get("ORDER_NO"), map.get("ORDER_ITEM_CODE"), "EXAM", map.get("ORDER_TIME"));
                order.put("overTime", true == isOverTime ? "超时" : "正常");
            }
            ColumnUtil.convertMapping(order, map, new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME",
                    "ORDER_DOCTOR_NAME", "ORDER_TIME", "ORDER_NO", "REPORT_STATUS", "APPLY_NO", "EXECSQN"});
            orders.add(order);
        }
        //重置分页结果
        page.setResult(orders);
        return page;
    }

    @Override
    public Page<Map<String, String>> getOperList(String patId, String visitId, String visitType, int pageNo,
                                                 int pageSize) {
        //过滤条件
        StringBuffer filterStr = new StringBuffer();
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //北医三院门诊医嘱特殊处理
        if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache("org_oid")) && "OUTPV".equals(visitType)) {
            //手术医嘱
            filterStr.append("ORDER_CLASS_NAME|like|术;");
            filterStr.append(Config.getVISIT_VIEW_OPERATION_CONFIG() + ";");
            page = getOperOrderListMZ(patId, visitId, null, filterStr.toString(), "ORDER_TIME", "desc", pageNo,
                    pageSize);
            //字段映射
            List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
            for (Map<String, String> map : page) {
                Map<String, String> order = new HashMap<String, String>();
                ColumnUtil.convertMapping(order, map, "ORDER_NO", "OPERATION_NAME", "ORDER_DOCTOR_NAME", "ORDER_TIME",
                        "PLAN_OPER_DOCTOR_NAME", "DIAG_BEFORE_OPERATION_NAME");
                //特殊字段转换
                Utils.checkAndPutToMap(order, "operationName", map.get("CHARGE_NAME"), "-", false); //手术名称
                //处理手术时间
                if (StringUtils.isNotBlank(map.get("PLAN_OPER_TIME"))) { //拟手术时间
                    order.put("operTime", map.get("PLAN_OPER_TIME"));
                } else {
                    Utils.checkAndPutToMap(order, "operTime", map.get("APPLY_OPER_TIME"), "-", false); //申请手术时间
                }
                orders.add(order);
            }
            //重置分页
            page.setResult(orders);
            return page;
        }
        //非北医三院门诊 正常查询
        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_OPER");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_OPER");
        }
        page = getOperOrderList(patId, visitId, visitType, orderClassStrings, "", "ORDER_TIME", "desc", pageNo,
                pageSize);
        //字段映射
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            ColumnUtil.convertMapping(order, map, new String[]{"ORDER_NO", "ORDER_DOCTOR_NAME", "ORDER_TIME",
                    "PLAN_OPER_DOCTOR_NAME", "DIAG_BEFORE_OPERATION_NAME"});
            //处理手术名称
            String operaName = map.get("OPERATION_NAME");
            if (StringUtils.isNotBlank(operaName)) {
                order.put("operationName", operaName);
            } else {
                Utils.checkAndPutToMap(order, "operationName", map.get("ORDER_ITEM_NAME"), "-", false);
            }
            //处理手术时间
            if (StringUtils.isNotBlank(map.get("PLAN_OPER_TIME"))) { //拟手术时间
                order.put("operTime", map.get("PLAN_OPER_TIME"));
            } else {
                Utils.checkAndPutToMap(order, "operTime", map.get("APPLY_OPER_TIME"), "-", false); //申请手术时间
            }
            orders.add(order);
        }
        //重置分页
        page.setResult(orders);
        return page;
    }

    @Override
    public Page<Map<String, String>> getBloodList(String patId, String visitId, int pageNo, int pageSize) {
        Page<Map<String, String>> page = getBloodApplyList(patId, visitId, "APPLY_DATE", "desc", pageNo, pageSize);
        //字段映射
        List<Map<String, String>> bloods = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> blood = new HashMap<String, String>();
            //处理申请单号
            Utils.checkAndPutToMap(blood, "timesNo", map.get("APPLY_NO"), "-", false);
            Utils.checkAndPutToMap(blood, "orderNo", map.get("APPLY_NO"), "-", false);
            ColumnUtil.convertMapping(blood, map, new String[]{"ORDER_NO", "APPLY_DATE", "APPLY_PERSON_NAME",
                    "APPLY_DEPT_NAME", "SQD_STATUS_NAME"});
            blood.put("orderType", "YX");
            bloods.add(blood);
        }
        //重置分页
        page.setResult(bloods);
        return page;
    }

    @Override
    public Page<Map<String, String>> getOrderExeList(String patId, String visitId, String orderNo, int pageNo,
                                                     int pageSize) {
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (pageSize == 0) {
            pageSize = 30;
        }
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        //计划执行时间 降序
        page.setOrderBy("PLAN_PRESC_TIME");
        page.setOrderDir("desc");
        //页面查询条件
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        createPropertyFilter("ORDER_NO", orderNo, MatchType.EQ.getOperation(), filters);
        //TODO 这里使用hbaseDao查询报错
        page = hbaseDao.findByConditionPage("HDR_ORDER_EXE", patId, visitId, filters, page,
                new String[]{"ORDER_NO", "PLAN_PRESC_TIME", "PLAN_PRESC_NURSE_NAME", "PRESC_TIME", "PRESC_NURSE_NAME",
                        "FINISH_TIME", "FINISH_NURSE_NAME"});

        //数据处理  字段映射
        List<Map<String, String>> exes = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            //仅保留 实际执行时间存在的
            if (StringUtils.isNotBlank(map.get("PRESC_TIME"))) {
                //字段映射
                Map<String, String> exe = new HashMap<String, String>();
                ColumnUtil.convertMapping(exe, map,
                        new String[]{"ORDER_NO", "PLAN_PRESC_TIME", "PLAN_PRESC_NURSE_NAME", "PRESC_TIME",
                                "PRESC_NURSE_NAME", "FINISH_TIME", "FINISH_NURSE_NAME"});
                exes.add(exe);
            }
        }
        //重置分页
        page.setResult(exes);
        return page;
    }

    @Override
    public Page<Map<String, String>> getDrugCheckList(String patId, String visitId, String orderNo, int pageNo,
                                                      int pageSize) {
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (pageSize == 0) {
            pageSize = 30;
        }
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        //医嘱发起时间 降序
        page.setOrderBy("OCC_TIME");
        page.setOrderDir("desc");
        //查询住院计费表 获取药品医嘱发药审核数据
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        createPropertyFilter("ORDER_NO", orderNo, MatchType.EQ.getOperation(), filters);
        createPropertyFilter("APPLY_STATUS_CODE", "1", MatchType.IN.getOperation(), filters); //已确认
        page = hbaseDao.findByConditionPage("HDR_IN_CHARGE", patId, visitId, filters, page, new String[]{"ORDER_NO",
                "CHARGE_CONFIRMER_NAME", "CHARGE_CONFIRM_TIME", "PAGE_NO", "OCC_TIME", "DETAIL_SN"});
        //数据处理 字段映射
        List<Map<String, String>> checks = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> check = new HashMap<String, String>();
            ColumnUtil.convertMapping(check, map, new String[]{"ORDER_NO", "CHARGE_CONFIRMER_NAME",
                    "CHARGE_CONFIRM_TIME", "PAGE_NO", "OCC_TIME", "DETAIL_SN"});
            checks.add(check);
        }
        //重置分页
        page.setResult(checks);
        return page;
    }

    @Override
    public Page<Map<String, String>> getLabReportDetails(String patId, String visitType, String field, String orderNo,
                                                         int pageNo, int pageSize) {
        Map<String, Object> result = new HashMap<String, Object>();
        String tableName = "HDR_LAB_REPORT_DETAIL";
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if ("OUTPV".equals(visitType)) {
            createPropertyFilter("VISIT_TYPE_CODE", "01", MatchType.EQ.getOperation(), filters);
        } else {
            createPropertyFilter("VISIT_TYPE_CODE", "02", MatchType.EQ.getOperation(), filters);
        }
        PropertyFilter pfOrderNo = new PropertyFilter();
        if (StringUtils.isNotBlank(orderNo)) {
            //          createPropertyFilter("ORDER_NO", orderNo, MatchType.EQ.getOperation(), filters);
            pfOrderNo = new PropertyFilter(field, "STRING", MatchType.EQ.getOperation(), orderNo);
            filters.add(pfOrderNo);
        }
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        if (pageSize == 0) {
            pageSize = 30;
        }
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        page.setOrderBy("REPORT_TIME");
        page.setOrderDir("desc");
        //检验大项,检验细项,检验定量结果,检验定性结果,报告时间，参考范围
        page = hbaseDao.findByConditionPage(tableName, patId, filters, page,
                new String[]{"LAB_ITEM_CODE", "LAB_SUB_ITEM_CODE", "LAB_SUB_ITEM_NAME", "REPORT_TIME", "RANGE",
                        "LAB_RESULT_VALUE", "LAB_RESULT_UNIT", "LAB_QUAL_RESULT", "RESULT_STATUS_CODE", "ORDER_NO",
                        "RANGE"});
        //        if(page.getTotalCount() <=0 && StringUtils.isNotBlank(orderNo)){
        //            filters.remove(pfOrderNo);
        //            createPropertyFilter("EXECSQN", orderNo, MatchType.EQ.getOperation(), filters);
        //            page = hbaseDao.findByConditionPage(tableName, preRowkey, filters, page, new String[]{"LAB_ITEM_CODE",
        //                    "LAB_SUB_ITEM_CODE", "LAB_SUB_ITEM_NAME", "REPORT_TIME", "RANGE", "LAB_RESULT_VALUE",
        //                    "LAB_RESULT_UNIT", "LAB_QUAL_RESULT", "RESULT_STATUS_CODE", "ORDER_NO", "RANGE"});
        //        }

/*		if (page.getTotalCount() <= 0) {
			result.put("status", "0");
			result.put("message", "未找到数据！");
			return result;
		}
		result.put("status", "1");*/
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            if (StringUtils.isNotBlank(map.get("LAB_SUB_ITEM_NAME"))) {
                Map<String, String> item = new HashMap<String, String>();
                ColumnUtil.convertMapping(item, map,
                        new String[]{"LAB_ITEM_CODE", "LAB_SUB_ITEM_CODE", "LAB_SUB_ITEM_NAME", "REPORT_TIME",
                                "RANGE", "LAB_RESULT_VALUE", "LAB_RESULT_UNIT", "LAB_QUAL_RESULT", "RESULT_STATUS_CODE",
                                "ORDER_NO", "RANGE"});
                items.add(item);
            }
        }
        //重置分页
        page.setResult(items);
//		result.put("list", page.getResult());
        return page;
    }

    @Override
    public Map<String, String> getExamReportDetails(String patId, String visitType, String field, String orderNo) {
        Map<String, String> result = new HashMap<String, String>();
        String tableName = "HDR_EXAM_REPORT";

        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        if ("OUTPV".equals(visitType)) {
            createPropertyFilter("VISIT_TYPE_CODE", "01", MatchType.EQ.getOperation(), filters);
        } else {
            createPropertyFilter("VISIT_TYPE_CODE", "02", MatchType.EQ.getOperation(), filters);
        }
        String operation = Config.getCIV_ORDER_EXAM_MATCHTYPE();
        createPropertyFilter(field, orderNo, operation, filters);
        //检验大项,检验细项,检验定量结果,检验定性结果,报告时间，参考范围
        List<Map<String, String>> list = hbaseDao.findConditionByKey(tableName, patId, filters,
                new String[]{"EXAM_FEATURE", "EXAM_DIAG", "EXAM_ITEM_NAME", "EXAM_ITEM_CODE", "REPORT_TIME",
                        "ORDER_ITEM_CODE", "ORDER_ITEM_NAME"});
        //未找到，终止执行
        if (list.size() == 0) {
            return result;
        }
        Map<String, String> map = list.get(0);
        //处理检查所见字段
        String examFeature = map.get("EXAM_FEATURE");
        if (StringUtils.isNotBlank(examFeature)) {
            //去掉 \X000d\
            if (examFeature.contains("\\X000d\\")) {
                result.put("examFeature", examFeature.replace("\\X000d\\", ""));
            } else {
                result.put("examFeature", examFeature);
            }
        } else {
            result.put("examFeature", "-");
        }
        //处理检查诊断字段
        String examDiag = map.get("EXAM_DIAG");
        if (StringUtils.isNotBlank(examDiag)) {
            //去掉 \X000d\
            if (examDiag.contains("\\X000d\\")) {
                result.put("examDiag", examDiag.replace("\\X000d\\", ""));
            } else {
                result.put("examDiag", examDiag);
            }
        } else {
            result.put("examDiag", "-");
        }
        return result;
    }

    @Override
    public long getOrderCount(String patientId, String visitId, String visitType) {
        //查询医嘱数量
		/*	Page<Map<String, String>> kf = getDrugListKF(patientId, visitId, visitType, "", "", "", "", "", 0, 0);
			Page<Map<String, String>> jmzs = getDrugListJMZS(patientId, visitId, visitType, "", "", "", "", "", 0, 0);
			Page<Map<String, String>> qt = getDrugListQTYP(patientId, visitId, visitType, "", "", 0, 0);
			Page<Map<String, String>> exam = getExamList(patientId, visitId, visitType, "", "", "", 0, 0);
			Page<Map<String, String>> lab = getLabList(patientId, visitId, visitType, "", "", "", 0, 0);
			Page<Map<String, String>> opera = getOperList(patientId, visitId, visitType, 0, 0);
			Page<Map<String, String>> blood = getBloodList(patientId, visitId, 0, 0);
			Page<Map<String, String>> other = getOthersOrderList(patientId, visitId, visitType, "", "", 0, 0);
			long kfCount = kf.getTotalCount() < 0 ? 0 : kf.getTotalCount(); //口服药品
			long jmzsCount = jmzs.getTotalCount() < 0 ? 0 : jmzs.getTotalCount(); //静脉药品
			long qtCount = qt.getTotalCount() < 0 ? 0 : qt.getTotalCount(); //其他药品
			long examCount = exam.getTotalCount() < 0 ? 0 : exam.getTotalCount(); //检查
			long labCount = lab.getTotalCount() < 0 ? 0 : lab.getTotalCount(); //检验
			long operaCount = opera.getTotalCount() < 0 ? 0 : opera.getTotalCount(); //手术
			long bloodCount = blood.getTotalCount() < 0 ? 0 : blood.getTotalCount(); //用血
			long otherCount = other.getTotalCount() < 0 ? 0 : other.getTotalCount(); //用血
			long count = kfCount + jmzsCount + qtCount + examCount + labCount + operaCount + bloodCount + otherCount;
			return count;
			*/
        Page<Map<String, String>> all = getVisitPageView(patientId, visitId, visitType, "", "", "", "", "", 0, 0);
        long allCount = all.getTotalCount() < 0 ? 0 : all.getTotalCount(); //所有医嘱
        return allCount;
    }

    @Override
    public Map<String, Object> getTypeOrderCount(String patientId, String visitId, String visitType) {
        Map<String, Object> result = new HashMap<String, Object>();
        //查询医嘱数量
        Page<Map<String, String>> kf = getDrugListKF(patientId, visitId, visitType, "", "", "", "", "", 0, 0);
        Page<Map<String, String>> jmzs = getDrugListJMZS(patientId, visitId, visitType, "", "", "", "", "", 0, 0);
        Page<Map<String, String>> qt = getDrugListQTYP(patientId, visitId, visitType, "", "", 0, 0);
        Page<Map<String, String>> exam = getExamList(patientId, visitId, visitType, "", "", "", 0, 0);
        Page<Map<String, String>> lab = getLabList(patientId, visitId, visitType, "", "", "", 0, 0);
        Page<Map<String, String>> opera = getOperList(patientId, visitId, visitType, 0, 0);
        Page<Map<String, String>> blood = getBloodList(patientId, visitId, 0, 0);
        Page<Map<String, String>> nurse = getNurseOrderList(patientId, visitId, visitType, "", "", 0, 0);
        Page<Map<String, String>> others = getOthersOrderList(patientId, visitId, visitType, "", "", 0, 0);
        Page<Map<String, String>> all = getVisitPageView(patientId, visitId, visitType, "", "", "", "", "", 0, 0);
        long kfCount = kf.getTotalCount() < 0 ? 0 : kf.getTotalCount(); //口服药品
        long jmzsCount = jmzs.getTotalCount() < 0 ? 0 : jmzs.getTotalCount(); //静脉药品
        long qtCount = qt.getTotalCount() < 0 ? 0 : qt.getTotalCount(); //静脉药品
        long examCount = exam.getTotalCount() < 0 ? 0 : exam.getTotalCount(); //检查
        long labCount = lab.getTotalCount() < 0 ? 0 : lab.getTotalCount(); //检验
        long operaCount = opera.getTotalCount() < 0 ? 0 : opera.getTotalCount(); //手术
        long bloodCount = blood.getTotalCount() < 0 ? 0 : blood.getTotalCount(); //用血
        long nurseCount = nurse.getTotalCount() < 0 ? 0 : nurse.getTotalCount(); //护理
        long othersCount = others.getTotalCount() < 0 ? 0 : others.getTotalCount(); //其他类别的医嘱
        long allCount = all.getTotalCount() < 0 ? 0 : all.getTotalCount(); //所有医嘱
        result.put("kfCount", kfCount);
        result.put("jmzsCount", jmzsCount);
        result.put("qtCount", qtCount);
        result.put("examCount", examCount);
        result.put("labCount", labCount);
        result.put("operaCount", operaCount);
        result.put("bloodCount", bloodCount);
        result.put("nurseCount", nurseCount);
        result.put("othersCount", othersCount);
        result.put("allCount", allCount);
        return result;
    }

    /**
     * 查询护理医嘱
     *
     * @param patId
     * @param visitId
     * @param visitType
     * @param orderStatus
     * @param orderProperty
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Override
    public Page<Map<String, String>> getNurseOrderList(String patId, String visitId, String visitType,
                                                       String orderStatus, String orderProperty, int pageNo, int pageSize) {
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //过滤条件
        StringBuffer filterStr = new StringBuffer();
        //用药方式
        if (StringUtils.isNotBlank(Config.getOCLJMZSFilter())) {
            if ("OUTPV".equals(visitType)) {
                filterStr.append(Config.getOCL_MZ_NurseFilter() + ";");
            } else if ("INPV".equals(visitType)) {
                filterStr.append(Config.getOCL_INPV_NurseFilter() + ";");
            }
        }
        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_NURSE");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_NURSE");
        }
        page = getOrderList(patId, visitId, visitType, orderClassStrings, filterStr.toString(), "", "ORDER_TIME",
                "desc", "", "", "", pageNo, pageSize);
        //字段映射
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            ColumnUtil.convertMapping(order, map,
                    new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME", "DOSAGE_VALUE", "DOSAGE_UNIT",
                            "PHARMACY_WAY_NAME", "FREQUENCY_NAME", "ORDER_PROPERTIES_NAME", "ORDER_DOCTOR_NAME",
                            "ORDER_TIME", "ORDER_BEGIN_TIME", "ORDER_END_TIME", "ORDER_STATUS_NAME", "ORDER_NO",
                            "PARENT_ORDER_NO", "GROUPSORTCOLUMN"});
            orders.add(order);
        }
        //重置分页结果
        page.setResult(orders);
        return page;
    }

    /**
     * 查询其他医嘱
     *
     * @param patId
     * @param visitId
     * @param visitType
     * @param orderStatus
     * @param orderProperty
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Override
    public Page<Map<String, String>> getOthersOrderList(String patId, String visitId, String visitType,
                                                        String orderStatus, String orderProperty, int pageNo, int pageSize) {
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        //过滤条件
        StringBuffer filterStr = new StringBuffer();

        if ("OUTPV".equals(visitType)) {
            filterStr.append(Config.getOCL_MZ_OthersFilter() + ";");
        } else if ("INPV".equals(visitType)) {
            filterStr.append(Config.getOCL_INPV_OthersFilter() + ";");
        }

        //TODO 区分门诊和住院医嘱类别
        List<String> orderClassStrings = new ArrayList<String>();
        if ("OUTPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_MZ_OTHERS");
        } else if ("INPV".equals(visitType)) {
            orderClassStrings = CivUtils.getOrderClass("OrderClose_OTHERS");
        }
        page = getOrderList(patId, visitId, visitType, orderClassStrings, filterStr.toString(), "", "ORDER_TIME",
                "desc", "", "", "", pageNo, pageSize);
        //字段映射
        List<Map<String, String>> orders = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page) {
            Map<String, String> order = new HashMap<String, String>();
            ColumnUtil.convertMapping(order, map,
                    new String[]{"ORDER_ITEM_NAME", "ORDER_CLASS_NAME", "DOSAGE_VALUE", "DOSAGE_UNIT",
                            "PHARMACY_WAY_NAME", "FREQUENCY_NAME", "ORDER_PROPERTIES_NAME", "ORDER_DOCTOR_NAME",
                            "ORDER_TIME", "ORDER_BEGIN_TIME", "ORDER_END_TIME", "ORDER_STATUS_NAME", "ORDER_NO",
                            "PARENT_ORDER_NO", "GROUPSORTCOLUMN"});
            orders.add(order);
        }
        //重置分页结果
        page.setResult(orders);
        return page;
    }

    /**
     * 方法描述: 查询患者医嘱(分类视图)
     *
     * @param patientId      患者编号
     * @param orderStatus    医嘱状态    （下达，审核，开始，撤销，停止）
     * @param orderProperty  医嘱性质    （1-临时    2-长期）
     * @param orderType      医嘱类型   KF-口服药   JM-静脉药  JY-检验  JC-检查  SS-手术   YX-临床用血
     * @param orderTimeBegin 开立开始时间
     * @param orderTimeEnd   开立结束时间
     * @param orderName        药品名称
     * @param outPatientId   门诊患者标识
     * @param visitType      就诊类型
     * @param orderBy        排序字段
     * @param orderDir       排序规则
     * @param pageNo         页码
     * @param pageSize       分页单位
     */
    public void getOrders(String patientId, String orderStatus, String orderProperty, String orderType,
                          String orderTimeBegin, String orderTimeEnd, String orderName, String orderBy, String orderDir, int pageNo,
                          int pageSize, String outPatientId, String visitType) {


        /*如果 patientId 不为空，将参数传给 getOrdersByPat 方法，查询数据并映射；*/

            getOrdersByPat(patientId, visitType, orderStatus, orderProperty, orderType, orderTimeBegin, orderTimeEnd,
                    orderName, orderBy, orderDir);

       /* 如果 outPatientId 不为空，以 "," 将多串 outPatientId 拆分开，再将每一部分都以"|" 分开，
            得到 visitType（就诊类型）和 patientId （患者编号），
                最后使用这些 visitType（就诊类型）和 patientId （患者编号）循环调用getOrdersByPat 方法，查询数据并映射返回给前端
        */
       /*查询结果以 "orderTime" 降序排列；*/

        }

    }

    public void getOrdersByPat(String patId, String visitType, String orderStatus, String orderProperty,
                               String orderType, String orderTimeBegin, String orderTimeEnd, String orderName, String orderby,
                               String orderdir) {
/*

        如果 visitType 等于 01 ，那么门诊医嘱处理流程为 括号1 {
            增设查询条件 "VISIT_TYPE_CODE" = visitType ；
            如果 orderProperty 不为空，那么 括号2 {
                如果 orderProperty = 1，那么增设查询条件 "ORDER_PROPERTIES_NAME" IN ORDER_SHORT_PROPERTY_CONFIG（配置项）；
                如果 orderProperty = 2，那么增设查询条件 "ORDER_PROPERTIES_NAME" IN ORDER_LONG_PROPERTY_CONFIG（配置项）；
            } 括号2
            如果 orderStatus 不为空，那么增设查询条件 "ORDER_STATUS_NAME" LIKE orderStatus;
            如果 orderTimeBegin 不为空，那么增设查询条件 "ORDER_TIME" >= orderTimeBegin ；
            如果 orderTimeEnd 不为空，那么增设查询条件 "ORDER_TIME" <= orderTimeEnd ；

			（以下过滤条件配置项格式：列名 | 操作符 | 参数1, 参数2..;
            列名 | 操作符 | 参数... #最后一项末尾不加英文分号 #）
			（以下查询条件 "ORDER_CLASS_CODE" 对应配置项格式为：参数1, 参数2, 参数3... #最后一项不加英文逗号 #）
            如果 orderType = "KF" ，那么 {
                增设查询条件 "ORDER_CLASS_CODE" IN OrderClose_MZ_DRUG_KF（配置项）；
                读取civ_config表中配置项 OCL_MZ_KFFilter ，加入过滤条件；
            }
            如果 orderType = "JM" ，那么 {
                增设查询条件 "ORDER_CLASS_CODE" IN OrderClose_MZ_DRUG_JMZS（配置项）；
                读取civ_config表中配置项 OCL_MZ_JMZSFilter，加入过滤条件；
            }
            如果 orderType = "QT" ，那么 {
                增设查询条件 "ORDER_CLASS_CODE" IN OrderClose_MZ_DRUG_QTYP（配置项）；
                读取civ_config表中配置项 OCL_MZ_QTFilter，加入过滤条件；
            }
            将过滤条件转化为查询条件；

            如果 orderName 不为空，增设查询条件 "ORDER_ITEM_NAME" LIKE orderName ；

            根据patientId，结合所有查询条件，从 HDR_OUT_ORDER 表中查询所有字段；
            将以下字段一一映射，{
				#返回给前端的字段< ------->数据库的字段 #
                "orderItemName" < ------ > "ORDER_ITEM_NAME" //医嘱名称
                "dosageValue" < ------ > "DOSAGE_VALUE" //剂量
                "dosageUnit" < ------ > "DOSAGE_UNIT" //单位1
                "pharmacyWayName" < ------ > "PHARMACY_WAY_NAME" //用法
                "frequencyName" < ------ > "FREQUENCY_NAME" //执行频次
                "orderPropertiesName" < ------ > "ORDER_PROPERTIES_NAME" //医嘱类型
                "orderDoctorName" < ------ > "ORDER_DOCTOR_NAME" //开立人
                "orderTime" < ------ > "ORDER_TIME" //开立时间
                "orderBeginTime" < ------ > "ORDER_BEGIN_TIME" //开始时间
                "orderEndTime" < ------ > "ORDER_END_TIME" //结束时间
                "orderStatusName" < ------ > "ORDER_STATUS_NAME" //医嘱状态
                "orderNo" < ------ > "ORDER_NO" //医嘱号
                "visitId" < ------ > "VISIT_ID" //就诊次
                "visitTypeCode" < ------ > "VISIT_TYPE_CODE" //就诊类型
                "oid" < ------ > "OID" //oid
                "patient_id" < ----->patientId;

                如果 orderType 等于 "JM" ，那么 {
                    "groupSortColumn" < ----->"GROUPSORTCOLUMN" //组标志
                    如果数据库中 "PARENT_ORDER_NO" 不为空，那么 "parentOrderNo" < ----->"PARENT_ORDER_NO";//父医嘱
                    否则 "parentOrderNo" < ----->"ORDER_NO";//父医嘱
                }
            }


        } 括号1 否则，均判定为住院医嘱，处理流程为 括号2{
            如果 orderProperty 不为空，那么 {
                如果 orderProperty = 1，那么增设查询条件 "ORDER_PROPERTIES_NAME" IN ORDER_SHORT_PROPERTY_CONFIG（配置项）；
                如果 orderProperty = 2，那么增设查询条件 "ORDER_PROPERTIES_NAME" IN ORDER_LONG_PROPERTY_CONFIG（配置项）；
            }
            如果 orderStatus 不为空，那么增设查询条件 "ORDER_STATUS_NAME" LIKE orderStatus;
            如果 orderTimeBegin 不为空，那么增设查询条件 "ORDER_TIME" >= orderTimeBegin ；
            如果 orderTimeEnd 不为空，那么增设查询条件 "ORDER_TIME" <= orderTimeEnd ；


			（以下过滤条件配置项格式：列名 | 操作符 | 参数1, 参数2..;
            列名 | 操作符 | 参数... #最后一项末尾不加英文分号 #）
			（以下查询条件 "ORDER_CLASS_CODE" 对应配置项格式为：参数1, 参数2, 参数3... #最后一项不加英文逗号 #）
            如果 orderType = "KF" ，那么 {
                增设查询条件 "ORDER_CLASS_CODE" IN OrderClose_DRUG_KF（配置项）；
                读取civ_config表中配置项 OCL_KFFilter ，加入过滤条件；
            }
            如果 orderType = "JM" ，那么 {
                增设查询条件 "ORDER_CLASS_CODE" IN OrderClose_DRUG_JMZS（配置项）；
                读取civ_config表中配置项 OCL_JMZSFilter，加入过滤条件；
            }
            如果 orderType = "QT" ，那么 {
                增设查询条件 "ORDER_CLASS_CODE" IN OrderClose_DRUG_QTYP（配置项）；
                读取civ_config表中配置项 OCL_QTFilter，加入过滤条件；
            }
            将过滤条件转化为查询条件；

            如果 orderName 不为空，增设查询条件 "ORDER_ITEM_NAME" LIKE orderName ；

            根据patientId，结合所有查询条件，从 HDR_IN_ORDER 表中查询所有字段；
            将以下字段一一映射，{
				#返回给前端的字段< ------->数据库的字段 #
                "orderItemName" < ------ > "ORDER_ITEM_NAME" //医嘱名称
                "dosageValue" < ------ > "DOSAGE_VALUE" //剂量
                "dosageUnit" < ------ > "DOSAGE_UNIT" //单位1
                "pharmacyWayName" < ------ > "PHARMACY_WAY_NAME" //用法
                "frequencyName" < ------ > "FREQUENCY_NAME" //执行频次
                "orderPropertiesName" < ------ > "ORDER_PROPERTIES_NAME" //医嘱类型
                "orderDoctorName" < ------ > "ORDER_DOCTOR_NAME" //开立人
                "orderTime" < ------ > "ORDER_TIME" //开立时间
                "orderBeginTime" < ------ > "ORDER_BEGIN_TIME" //开始时间
                "orderEndTime" < ------ > "ORDER_END_TIME" //结束时间
                "orderStatusName" < ------ > "ORDER_STATUS_NAME" //医嘱状态
                "orderNo" < ------ > "ORDER_NO" //医嘱号
                "visitId" < ------ > "VISIT_ID" //就诊次
                "visitTypeCode" < ------ > "VISIT_TYPE_CODE" //就诊类型
                "oid" < ------ > "OID" //oid
                "patient_id" < ----->patientId;

                如果 orderType 等于 "JM" ，那么 {
                    "groupSortColumn" < ----->"GROUPSORTCOLUMN" //组标志
                    如果数据库中 "PARENT_ORDER_NO" 不为空，那么 "parentOrderNo" < ----->"PARENT_ORDER_NO";//父医嘱
                    否则 "parentOrderNo" < ----->"ORDER_NO";//父医嘱
                }
        }括号2
*/



    }



    @Override
    public void getOrdersNum(String patId, Map<String, Object> resultMap, String orderType, String outPatientId,
                             String visitType) {

        int num = 0;
        if (StringUtils.isNotBlank(patId)) {
            num = num + getOrdersByPat(patId, visitType, orderType);
        }

        if (StringUtils.isNotBlank(outPatientId)) {
            String[] pats = outPatientId.split(",");
            for (int i = 0; i < pats.length; i++) {
                if (StringUtils.isNotBlank(pats[i])) {
                    String[] pat = pats[i].split("\\|");
                    num = num + getOrdersByPat(pat[1], pat[0], orderType);
                }
            }
        }
        resultMap.put("num", num);

    }

    public int getOrdersByPat(String patId, String visitType, String orderType) {
        int num = 0;
        StringBuffer filterStr = new StringBuffer(); //过滤条件
        //查询条件
        if ("01".equals(visitType)) {
            List<PropertyFilter> filters = new ArrayList<PropertyFilter>();

            String tableName = "";
            if (HdrConstantEnum.HOSPITAL_BYSY.getCode().equals(ConfigCache.getCache("org_oid"))) {
                tableName = HdrTableEnum.HDR_OUT_CHARGE.getCode();
                List<PropertyFilter> BYSYfilter = new ArrayList<PropertyFilter>();
                for (PropertyFilter propertyFilter : filters) {
                    BYSYfilter.add(propertyFilter);
                }
                //医嘱类型   KF-口服药   JM-静脉药  JY-检验  JC-检查  SS-手术   YX-临床用血
                if ("KF".equals(orderType)) { //口服药品
                    createPropertyFilter("PHARMACY_WAY_NAME", "口服", MatchType.LIKE.getOperation(), BYSYfilter);
                } else if ("JM".equals(orderType)) { //静脉药物
                    createPropertyFilter("PHARMACY_WAY_NAME", "静脉", MatchType.LIKE.getOperation(), BYSYfilter);
                }
                List<String> types = CivUtils.getOrderClass("BYSY_MZ_DRUG");
                //医嘱类别   药品，检查，检验...
                if (types != null && types.size() > 0) {
                    String orderClassString = "";
                    for (String orderString : types) {
                        orderClassString += orderString + ",";
                    }
                    orderClassString = orderClassString.substring(0, orderClassString.length() - 1);
                    createPropertyFilter("ORDER_CLASS_CODE", orderClassString, MatchType.IN.getOperation(), BYSYfilter);
                }
                //门诊医嘱
                List<Map<String, String>> page = hbaseDao.findConditionByPatient(tableName, patId, BYSYfilter,
                        new String[]{"ORDER_NO", "VISIT_ID"});
                num = num + page.size();

            } else {
                List<String> types = new ArrayList<String>();
                //医嘱类型   KF-口服药   JM-静脉药  JY-检验  JC-检查  SS-手术   YX-临床用血
                if ("KF".equals(orderType)) { //口服药品
                    types = CivUtils.getOrderClass("OrderClose_MZ_DRUG_KF");
                    filterStr.append(Config.getOclMzKffilter() + ";");
                } else if ("JM".equals(orderType)) { //静脉药物
                    types = CivUtils.getOrderClass("OrderClose_MZ_DRUG_JMZS");
                    //静脉药物识别码
                    filterStr.append(Config.getOclMzJmzsfilter() + ";");
                } else if ("QT".equals(orderType)) {//其他药物
                    types = CivUtils.getOrderClass("OrderClose_MZ_DRUG_QTYP");
                    //其他
                    filterStr.append(Config.getOclMzQTfilter() + ";");
                }
                strToFilter(filterStr.toString(), filters, ";");
                if (null != types && types.size() > 0) {
                    String orderClass = "";
                    //拼接医嘱类型
                    for (String type : types) {
                        orderClass = orderClass + type + ",";
                    }
                    orderClass = orderClass.substring(0, orderClass.length() - 1);
                    createPropertyFilter("ORDER_CLASS_CODE", orderClass, MatchType.IN.getOperation(), filters);
                }
                tableName = HdrTableEnum.HDR_OUT_ORDER.getCode();
                //门诊医嘱
                List<Map<String, String>> outOrders = hbaseDao.findConditionByPatient(tableName, patId, filters
                );
                num = num + outOrders.size();
            }
        } else {
            List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
            filterStr = new StringBuffer();
            //住院医嘱
            List<String> types = new ArrayList<String>();
            //医嘱类型   KF-口服药   JM-静脉药  JY-检验  JC-检查  SS-手术   YX-临床用血
            if ("KF".equals(orderType)) { //口服药品
                types = CivUtils.getOrderClass("OrderClose_DRUG_KF");
                filterStr.append(Config.getOCLKFFilter() + ";");
            } else if ("JM".equals(orderType)) { //静脉药物
                types = CivUtils.getOrderClass("OrderClose_DRUG_JMZS");
                //静脉药物识别码
                filterStr.append(Config.getOCLJMZSFilter() + ";");
            } else if ("QT".equals(orderType)) {//其他药物
                types = CivUtils.getOrderClass("OrderClose_DRUG_QTYP");
                //其他
                filterStr.append(Config.getOCLQTFilter() + ";");
            }
            strToFilter(filterStr.toString(), filters, ";");
            if (null != types && types.size() > 0) {
                String orderClass = "";
                //拼接医嘱类型
                for (String type : types) {
                    orderClass = orderClass + type + ",";
                }
                orderClass = orderClass.substring(0, orderClass.length() - 1);
                createPropertyFilter("ORDER_CLASS_CODE", orderClass, MatchType.IN.getOperation(), filters);
            }
            //排除状态为撤销的医嘱
            //			createPropertyFilter("ORDER_STATUS_NAME", "撤销,废弃", MatchType.NOTIN.getOperation(), filters);
            //住院医嘱
            List<Map<String, String>> inpOrders = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_IN_ORDER.getCode(),
                    patId, filters);
            num = num + inpOrders.size();
        }

        return num;
    }

    /**
     * 获取患者末次就诊显示配置
     *
     * @return
     */
    public Map<String, String> getPatLastInfoViewConfig() {
        Map<String, String> confgiMap = new HashMap<String, String>();
        confgiMap.put("name", "姓名");
        confgiMap.put("birthday", "出生年月");
        confgiMap.put("patient_id", "患者ID");
        //就诊号  门诊显示
        confgiMap.put("visit_no", "门诊号");
        //就诊号  住院显示
        confgiMap.put("in_no", "住院号");
        confgiMap.put("visit_dept", "科室");
        //就诊日期 门诊显示
        confgiMap.put("visit_time", "就诊日期");
        //就诊日期 住院显示
        confgiMap.put("admission_time", "入院日期");
        confgiMap.put("sex", "性别");
        confgiMap.put("bed_no", "床号");
        confgiMap.put("main_diag", "主诊断");
        confgiMap.put("visit_num", "全部就诊");
        String configStr = Config.getPATIENT_LAST_VISIT_INFO_VIEW();
        if (org.apache.commons.lang.StringUtils.isNotBlank(configStr)) {
            String[] configs = configStr.split(";");
            for (String viewItem : configs) {
                if (org.apache.commons.lang.StringUtils.isNotBlank(viewItem) && viewItem.contains("=")) {
                    String[] viewItems = viewItem.split("=");
                    confgiMap.put(viewItems[0], viewItems[1]);
                }
            }
        }
        return confgiMap;
    }

    @Override
    public Page<Map<String, String>> getVisitPageView(String patientId, String visitId, String visitType,
                                                      String orderStatus, String orderProperty, String orderType, String orderBy, String orderDir, int pageNo,
                                                      int pageSize) {
        String tableName = null;
        if ("OUTPV".equals(visitType)) {
            tableName = HdrTableEnum.HDR_OUT_ORDER.getCode();
        } else if ("INPV".equals(visitType)) {
            tableName = HdrTableEnum.HDR_IN_ORDER.getCode();
        }

        if (StringUtil.isEmpty(orderBy)) {
            orderBy = "ORDER_TIME";
        }
        if (StringUtil.isEmpty(orderDir)) {
            orderDir = "desc";
        }
        if (pageNo == 0) {
            pageNo = 1;
        }
        if (pageSize == 0) {
            pageSize = 10;
        }
        //按条件查询
        List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
        PropertyFilter filter1 = new PropertyFilter();
        filter1.setMatchType("=");
        filter1.setPropertyName("VISIT_ID");
        filter1.setPropertyValue(visitId);
        filter1.setPropertyType("STRING");
        filters.add(filter1);
        StringBuffer filterStr = new StringBuffer();
        //医嘱性质   1-临时    2-长期
        if (StringUtils.isNotBlank(orderProperty)) {
            if ("1".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_SHORT_PROPERTY_CONFIG() + ";");
            } else if ("2".equals(orderProperty)) {
                filterStr.append("ORDER_PROPERTIES_NAME|in|" + Config.getORDER_LONG_PROPERTY_CONFIG() + ";");
            }
        }
        //医嘱状态
        if (StringUtils.isNotBlank(orderStatus)) {
            filterStr.append("ORDER_STATUS_NAME|like|" + orderStatus + ";");
        }
        //医嘱性质 和 医嘱状态 或其他条件
        strToFilter(filterStr.toString(), filters, ";");
        Page<Map<String, String>> page = new Page<Map<String, String>>();
        page.setOrderBy(orderBy);
        page.setOrderDir(orderDir);
        page.setPageNo(1);
        page.setPageSize(100000);
        Page<Map<String, String>> resultPage = hbaseDao.findPageConditionByPatient(tableName, patientId, page, filters);
        //解决先分页再排序问题
        List<Map<String, String>> listRes = resultPage.getResult();
        Utils.sortListByDate(listRes, "ORDER_TIME", Page.Sort.DESC);
        //分页
        ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(listRes, pageNo, pageSize);
        page.setResult(listPage.getPagedList());
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        //字段转换
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        for (Map<String, String> map : page.getResult()) {
            Map<String, String> order = new HashMap<String, String>();
            Utils.checkAndPutToMap(order, "orderClassName", map.get("ORDER_CLASS_NAME"), "-", false);//医嘱类别
            Utils.checkAndPutToMap(order, "orderPropertiesName", map.get("ORDER_PROPERTIES_NAME"), "-", false); //医嘱性质
            Utils.checkAndPutToMap(order, "orderItemName", map.get("ORDER_ITEM_NAME"), "-", false); //医嘱项
            Utils.checkAndPutToMap(order, "orderBeginTime", map.get("ORDER_BEGIN_TIME"), "-", false); //医嘱开始时间
            Utils.checkAndPutToMap(order, "orderEndTime", map.get("ORDER_END_TIME"), "-", false); //医嘱结束时间
            Utils.checkAndPutToMap(order, "frequencyName", map.get("FREQUENCY_NAME"), "-", false); //执行频次
            Utils.checkAndPutToMap(order, "durationValue", map.get("DURATION_VALUE"), "-", false); //持续时间
            Utils.checkAndPutToMap(order, "orderStatusName", map.get("ORDER_STATUS_NAME"), "-", false); //医嘱状态
            Utils.checkAndPutToMap(order, "orderType", getOrderType(visitType, map.get("ORDER_CLASS_CODE")), "-", false); //医嘱类别
            Utils.checkAndPutToMap(order, "orderNo", map.get("ORDER_NO"), "-", false); //医嘱类别
            Utils.checkAndPutToMap(order, "execsqn", map.get("EXECSQN"), "-", false); //医嘱类别
            list.add(order);
        }
        page.setResult(list);
        page.setTotalCount(resultPage.getTotalCount());
        return page;
    }

    //医嘱类别，属于哪个底下的医嘱查看用
    public static String getOrderType(String visitType, String orderClassCode) {
        if (StringUtils.isBlank(orderClassCode)) {
            return "OTHERS";
        }
        String KF = "";
        String JMZS = "";
        String QTYP = "";
        String LAB = "";
        String EXAM = "";
        String OPER = "";
        if ("OUTPV".equals(visitType)) {
            KF = CivUtils.getOrderClass("OrderClose_MZ_DRUG_KF").toString();
            JMZS = CivUtils.getOrderClass("OrderClose_MZ_DRUG_JMZS").toString();
            QTYP = CivUtils.getOrderClass("OrderClose_MZ_DRUG_QTYP").toString();
            LAB = CivUtils.getOrderClass("OrderClose_MZ_LAB").toString();
            EXAM = CivUtils.getOrderClass("OrderClose_MZ_EXAM").toString();
            OPER = CivUtils.getOrderClass("OrderClose_MZ_OPER ").toString();
        } else if ("INPV".equals(visitType)) {
            KF = CivUtils.getOrderClass("OrderClose_DRUG_KF").toString();
            JMZS = CivUtils.getOrderClass("OrderClose_DRUG_JMZS").toString();
            QTYP = CivUtils.getOrderClass("OrderClose_DRUG_QTYP").toString();
            LAB = CivUtils.getOrderClass("OrderClose_LAB").toString();
            EXAM = CivUtils.getOrderClass("OrderClose_EXAM").toString();
            OPER = CivUtils.getOrderClass("OrderClose_OPER").toString();
        }
        if (StringUtils.isNotBlank(KF) && KF.contains(orderClassCode)) {
            return "KF";
        } else if (StringUtils.isNotBlank(JMZS) && JMZS.contains(orderClassCode)) {
            return "JM";
        } else if (StringUtils.isNotBlank(QTYP) && QTYP.contains(orderClassCode)) {
            return "QT";
        } else if (StringUtils.isNotBlank(LAB) && LAB.contains(orderClassCode)) {
            return "JY";
        } else if (StringUtils.isNotBlank(EXAM) && EXAM.contains(orderClassCode)) {
            return "JC";
        } else if (StringUtils.isNotBlank(OPER) && OPER.contains(orderClassCode)) {
            return "SS";
        } else {
            return "OTHERS";
        }
    }
}
