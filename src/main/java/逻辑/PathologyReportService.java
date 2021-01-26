package 逻辑;


import java.util.*;


public class PathologyReportService  {




	private HbaseDao hbaseDao;

	private PowerService powerService;


	private PersonalConfigService personalConfigService;


	private SpecialtyViewPowerService specialtyViewPowerService;

	@Override
	public Page<Map<String, String>> getPathologyReportList(String patId, String visitId, String visitType,
			String orderBy, String orderDir, String mainDiag, String deptCode, int pageNo, int pageSize) {
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
			page.setOrderDir(Sort.DESC);
		} else {
			page.setOrderBy(orderBy);
			page.setOrderDir(orderDir);
		}
		try {
			if ("INPV".equals(visitType)) { //住院病理报告
				page = getInpvExamReports(page, patId, visitId, mainDiag, deptCode, pageable);
			} else if ("OUTPV".equals(visitType)) { //门诊病理报告
				page = getOutpvExamReports(page, patId, visitId, mainDiag, deptCode, pageable);
			}
		} catch (Exception e) {
			logger.error("查询hbase数据库失败！ ", e);
			throw new ApplicationException("查询病理报告失败！" + e.getCause());
		}
		//字段映射
		List<Map<String, String>> exams = new ArrayList<Map<String, String>>();
		for (Map<String, String> map : page) {
			//处理病理诊断
			String examDiag = map.get("EXAM_DIAG");
			if (StringUtils.isNotBlank(examDiag)) {
				//去掉 \X000d\
				if (examDiag.contains("\\X000d\\")) {
					map.put("EXAM_DIAG", examDiag.replace("\\X000d\\", ""));
				}
			}
			Map<String, String> exam = new HashMap<String, String>();
			ColumnUtil.convertMapping(exam, map,
					new String[] {  "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_DIAG" ,"REPORT_NO"});
			exams.add(exam);
		}
		//重置分页
		page.setResult(exams);

		return page;
	}

	/**
	 * @param page      分页对象
	 * @param patientId 患者编号
	 * @param visitId   就诊次数
	 * @param pageable  是否分页
	 * @return 分页对象
	 * @Description 方法描述: 查询患者某次门诊的病理报告
	 */
	private Page<Map<String, String>> getOutpvExamReports(Page<Map<String, String>> page, String patientId,
			String visitId, String mainDiag, String deptCode, boolean pageable) {
		List<Map<String, String>> exams = new ArrayList<Map<String, String>>();
		//优先 根据vid查询
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		//病理报告权限筛选
		String usercode = SecurityUtils.getCurrentUserName(); //在线用户
		Map<String, Object> power = powerService.getPowerConfigByPathology(usercode);
		if ("false".equals(power.get("isAll"))) {
			List<String> typeList = (List<String>) power.get("power");
			String typeStr = StringUtils.join(typeList.toArray(), ",");
			filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.IN.getOperation(), typeStr));
		}
		filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.NOTIN.getOperation(),
				Config.getCIV_EXAM_VALUE().replace("/", ",")));
		filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
		filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), "01"));
		Config.setExamStatusFilter(filters);
		//新增专科配置，改成暂时先不分页查询
		int pageNo = page.getPageNo();
		int pageSize = page.getPageSize();
		//分页判断
		if (pageable) {
			page.setPageNo(1);
			page.setPageSize(10000);
			page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(), patientId, page, filters,
					 "EXAM_ITEM_CODE", "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_DIAG", "ORDER_NO","REPORT_NO");
		} else {
			exams = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(), patientId, filters,
					 "EXAM_ITEM_CODE", "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_DIAG", "ORDER_NO","REPORT_NO");
			Utils.sortListByDate(exams, page.getOrderBy(), page.getOrderDir()); //排序
			page.setResult(exams);
			page.setTotalCount(exams.size());
		}

		//TODO 若缺失就诊次，可根据CivUtils中的其他条件筛选
		//排序完之后合并分页
		handlerPage(patientId, visitId, page, mainDiag, deptCode, "Exam", pageable, pageNo, pageSize);
		return page;
	}

	/**
	 * @param page      分页对象
	 * @param patientId 患者编号
	 * @param visitId   就诊次数
	 * @param pageable  是否分页
	 * @return 分页对象
	 * @Description 方法描述: 查询患者某次住院的病理报告
	 */
	private Page<Map<String, String>> getInpvExamReports(Page<Map<String, String>> page, String patientId,
			String visitId, String mainDiag, String deptCode, boolean pageable) {
		List<Map<String, String>> exams = new ArrayList<Map<String, String>>();
		//优先 根据vid查询
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		//病理报告权限筛选
		String usercode = SecurityUtils.getCurrentUserName(); //在线用户
		//String usercode = "admin";
		Map<String, Object> power = powerService.getPowerConfigByPathology(usercode);
		if ("false".equals(power.get("isAll"))) {
			List<String> typeList = (List<String>) power.get("power");
			String typeStr = StringUtils.join(typeList.toArray(), ",");
			filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.IN.getOperation(), typeStr));
		}
		filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.NOTIN.getOperation(),
				Config.getCIV_EXAM_VALUE().replace("/", ",")));
		filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
		filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), "02"));
		/*filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.NOTIN.getOperation(),
				Config.getCIV_EMR_VALUE().replace("/", ",")));*/
		Config.setExamStatusFilter(filters);
		//新增专科配置，改成暂时先不分页查询,因HBASE查询工具有问题。
		int pageNo = page.getPageNo();
		int pageSize = page.getPageSize();
		//分页判断
		if (pageable) {
			page.setPageNo(1);
			page.setPageSize(10000);
			page = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(), patientId, page, filters,
					 "EXAM_ITEM_CODE", "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_DIAG", "ORDER_NO","REPORT_NO");
		} else {
			exams = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(), patientId, filters,
					 "EXAM_ITEM_CODE", "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_DIAG", "ORDER_NO","REPORT_NO");
			Utils.sortListByDate(exams, page.getOrderBy(), page.getOrderDir()); //排序
			page.setResult(exams);
			page.setTotalCount(exams.size());
		}
		//上述未查到，再根据入院出院时间查询
		if (page.getTotalCount() <= 0) {
			page.setPageNo(1);
			page.setPageSize(10000);
			page = CivUtils.getInpExamReports(page, patientId, visitId, pageable, power);
		}
		//排序完之后合并分页
		handlerPage(patientId, visitId, page, mainDiag, deptCode, "Exam", pageable, pageNo, pageSize);
		return page;
	}

	@Override
	public List<Map<String, String>> getPathologyOrderByPid(String pid, String vid, String order) {
		List<Map<String, String>> exams = new ArrayList<Map<String, String>>();
		//优先 根据vid查询
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		if (StringUtils.isNotBlank(order)) {
			filters.add(new PropertyFilter("ORDER_NO", "STRING", MatchType.EQ.getOperation(), order));
		}
		filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), vid));
		exams = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_IN_ORDER.getCode(), pid, filters,
				new String[] { "ORDER_ITEM_CODE", "ORDER_ITEM_NAME", "IN_PATIENT_ID", "VISIT_ID", "ORDER_NO" });
		return exams;
	}

	/**
	 * 查询专科设置处理结果集
	 */
	public void handlerPage(String patientId, String vid, Page<Map<String, String>> page, String mainDiag,
			String deptCode, String orderCode, boolean isPage, int pageNo, int pageSize) {
		if (StringUtils.isBlank(mainDiag) && StringUtils.isBlank(deptCode)) {
			return;
		}
		//查询专科视图配置  用药医嘱
		List<Map<String, String>> listDept = specialtyViewPowerService.getSpecialtyConfig(mainDiag, orderCode,
				deptCode);
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		List<Map<String, String>> dataTemp = new ArrayList<Map<String, String>>();
		for1: for (Map<String, String> map : page) {
			//病理需要再次关联下病理医嘱，因为专科病理配置的是病理医嘱
			List<Map<String, String>> exams = getPathologyOrderByPid(patientId, vid, map.get("ORDER_NO"));
			for2: for (Map<String, String> mapOrder : exams) {
				String orderCodeTemp = mapOrder.get("ORDER_ITEM_CODE");
				for3: for (Map<String, String> mapSpecialty : listDept) {
					String code = mapSpecialty.get("subItemCode");
					if (StringUtils.isNotBlank(orderCodeTemp) && orderCodeTemp.equals(code)) {
						data.add(map);
						continue for1;
					}
				}
			}
			dataTemp.add(map);

		}
		//合并数据
		for (Map<String, String> map : dataTemp) {
			data.add(map);
		}
		if (isPage) {
			ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(data, pageNo, pageSize);
			data = listPage.getPagedList();
		}
		page.setResult(data);
	}

	@Override
	public Map<String, String> getPathologyReportDetails(String pid,String vid,String reportNo) {
		Map<String, String> result = new HashMap<String, String>();
		//查询病理报告
//		Map<String, String> map = hbaseDao.getByKey(HdrTableEnum.HDR_EXAM_REPORT.getCode(), rowkey,
//				new String[] { "EXAM_CLASS_NAME", "EXAM_ITEM_NAME", "ORDER_NO", "EXAM_PART", "EXAM_PERFORM_TIME",
//						"EXAM_FEATURE", "EXAM_DIAG", "REPORT_TIME", "REPORT_DOCTOR_NAME", "PACS_URL", "VISIT_TYPE_CODE",
//						"EXAM_CLASS_CODE", "OUTP_NO", "INP_NO", "PACS_IMG_URL" });
		//查询字段配置
		String[] fields = new String[] { "EXAM_CLASS_NAME", "EXAM_ITEM_NAME", "ORDER_NO", "EXAM_PART", "EXAM_PERFORM_TIME",
				"EXAM_FEATURE", "EXAM_DIAG", "REPORT_TIME", "REPORT_DOCTOR_NAME", "PACS_URL", "VISIT_TYPE_CODE",
				"EXAM_CLASS_CODE", "OUTP_NO", "INP_NO", "PACS_IMG_URL" };
		String[] configField = Config.getCIV_PATHOLOGY_REPORT_URL_CONFIG_FIELDS();
		String[] fieldQuery = ArrayUtils.addAll(fields,configField);
		//优先 根据vid查询
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), vid));
		filters.add(new PropertyFilter("REPORT_NO", "STRING", MatchType.EQ.getOperation(), reportNo));
		List<Map<String,String>> list = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(), pid, filters,fieldQuery
				);
		//未找到，终止执行
		if (null==list || list.isEmpty()) {
			return result;
		}
		Map<String, String> map = list.get(0);
		//处理病理所见字段
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
		//处理病理诊断字段
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
		//获取pacsurl
		personalConfigService.getPacsUrl(map);
		//字段映射
		ColumnUtil.convertMapping(result, map,fieldQuery);
		//新增影像浏览url和pacsurl
		//map.put("imgUrl",map.get("PACS_IMG_URL"));
		List<Map<String, String>> urlList = new ArrayList<Map<String, String>>();
		List<Map<String, String>> fieldList = Config.getCIV_PATHOLOGY_REPORT_URL_CONFIG();
		for (Map<String, String> fieldInfo : fieldList) {
			Map<String, String> pacsUrl = new HashMap<String, String>();
			pacsUrl.put("name", fieldInfo.get("name"));
			pacsUrl.put("url", map.get(fieldInfo.get("field")));
			urlList.add(pacsUrl);
		}
		result.put("urls", JsonUtil.getJSONString(urlList));
		return result;
	}

	@Override
	public long getPathologyCount(String patientId, String visitId, String visitType) {
		//查询病理报告
		Page<Map<String, String>> page = getPathologyReportList(patientId, visitId, visitType, "", "", "", "", 0, 0);
		long checkCount = page.getTotalCount();
		if (checkCount < 0) {
			checkCount = 0;
		}
		return checkCount;
	}

	@Override
	public List<Map<String, Object>> getPathologyReports(String patientId, int pageNo, int pageSize, String orderby,
			String orderdir, String outPatientId, String visitType, String year, String key, String type,
			String click_Type) {
		List<Map<String, String>> exams = new ArrayList<Map<String, String>>();
		List<Map<String, String>> types = getPathologyReportTypes(patientId, outPatientId, visitType);

		if (StringUtils.isBlank(year)) {
			Calendar date = Calendar.getInstance();
			year = String.valueOf((date.get(Calendar.YEAR) + 1));
		}
		int num = 0;
		while (exams.size() == 0) {
			year = (Integer.valueOf(year) - 1) + "";
			if (StringUtils.isNotBlank(patientId)) {
				getCheckReportsByPat(patientId, visitType, orderby, orderdir, exams, types, year, key, type,
						click_Type);
			}
			if (StringUtils.isNotBlank(outPatientId)) {
				String[] pats = outPatientId.split(",");
				for (int i = 0; i < pats.length; i++) {
					if (StringUtils.isNotBlank(pats[i])) {
						String[] pat = pats[i].split("\\|");
						getCheckReportsByPat(pat[1], pat[0], orderby, orderdir, exams, types, year, key, type,
								click_Type);
					}
				}
			}
			num++;
			if (num == 20)
				break;
		}

		//列表
		Map<String, Object> resultList = new HashMap<String, Object>();
		//按报告时间月份分组
		CivUtils.groupByDate(resultList, exams, "reportTime");
		List<Map<String, Object>> listResult = new ArrayList<Map<String, Object>>();
		for (String time : resultList.keySet()) {
			Map<String, Object> rs = new HashMap<String, Object>();
			rs.put("time", time);
			rs.put("order", Integer.valueOf(CivUtils.changeFormatDate(time)));
			rs.put("data", resultList.get(time));
			listResult.add(rs);
		}
		Utils.sortListByDate(listResult, "order", Sort.DESC);
		return listResult;
	}

	/**
	 * @param patId    患者编号
	 * @param orderBy  排序字段
	 * @param orderDir 数据集合
	 * @param orderDir 报告类型
	 * @return 分页对象
	 * @Description 方法描述: 根据患者ID查询所有的 病理报告
	 */
	public void getCheckReportsByPat(String patientId, String visitType, String orderby, String orderdir,
			List<Map<String, String>> exams, List<Map<String, String>> types, String year, String key, String filed,
			String click_Type) {
		//分页
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(1);
		page.setPageSize(1000);
		//排序
		page.setOrderBy(orderby);
		page.setOrderDir(orderdir);
		//查询条件  按照病理项查询
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
		if (StringUtils.isNotBlank(year)) {
			PropertyFilter filter1 = new PropertyFilter("REPORT_TIME", "STRING", MatchType.GE.getOperation(),
					year + "-01-01");
			filters.add(filter1);
			if ("more".equals(click_Type)) {
				PropertyFilter filter2 = new PropertyFilter("REPORT_TIME", "STRING", MatchType.LE.getOperation(),
						year + "-12-31");
				filters.add(filter2);
			}
		}
		if (StringUtils.isNotBlank(key)) {
			if ("1".equals(filed)) {
				PropertyFilter filterKey = new PropertyFilter("EXAM_ITEM_NAME", "STRING", MatchType.LIKE.getOperation(),
						key);
				filters.add(filterKey);
			} else if ("2".equals(filed)) {
				PropertyFilter filterKey = new PropertyFilter("EXAM_DIAG", "STRING", MatchType.LIKE.getOperation(),
						key);
				filters.add(filterKey);
			} else if ("3".equals(filed)) {
				PropertyFilter filterKey = new PropertyFilter("EXAM_FEATURE", "STRING", MatchType.LIKE.getOperation(),
						key);
				filters.add(filterKey);
			}
		}
		//病理报告权限筛选
		String usercode = SecurityUtils.getCurrentUserName(); //在线用户
		Map<String, Object> power = powerService.getPowerConfigByPathology(usercode);
		if ("false".equals(power.get("isAll"))) {
			List<String> typeList = (List<String>) power.get("power");
			String typeStr = StringUtils.join(typeList.toArray(), ",");
			filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.IN.getOperation(), typeStr));
		}
		Config.setExamStatusFilter(filters);
		filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.NOTIN.getOperation(),
				Config.getCIV_EXAM_VALUE().replace("/", ",")));
		Page<Map<String, String>> result = hbaseDao.findPageConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(),
				patientId, page, filters, new String[] {  "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_CLASS_NAME",
						"PACS_URL", "EXAM_DIAG", "EXAM_PART", "REPORT_CONFIRM_TIME","REPORT_NO","OUT_PATIENT_ID","IN_PATIENT_ID","VISIT_ID" });
		List<Map<String, String>> list = result.getResult();
		//报告类型
		for (Map<String, String> map : list) {
			Map<String, String> exam = new HashMap<String, String>();
//			Utils.checkAndPutToMap(exam, "rowkey", map.get("ROWKEY"), "-", false); //rowkey
			Utils.checkAndPutToMap(exam, "examItemName", map.get("EXAM_ITEM_NAME"), "-", false); //病理项目
			Utils.checkAndPutToMap(exam, "reportNo", map.get("REPORT_NO"), "-", false); //病理报告号
			if(StringUtils.isNotBlank(map.get("IN_PATIENT_ID"))) {
				Utils.checkAndPutToMap(exam, "inPatientId", map.get("IN_PATIENT_ID"), "-", false); //患者号
			}else{
				Utils.checkAndPutToMap(exam, "inPatientId", map.get("OUT_PATIENT_ID"), "-", false); //患者号
			}
			Utils.checkAndPutToMap(exam, "visitid", map.get("VISIT_ID"), "-", false); //就诊号
			if (map.get("REPORT_TIME") == null)
				Utils.checkAndPutToMap(exam, "reportTime", map.get("REPORT_CONFIRM_TIME"), "-", false); //报告时间
			else
				Utils.checkAndPutToMap(exam, "reportTime", map.get("REPORT_TIME"), "-", false); //报告时间

			Utils.checkAndPutToMap(exam, "pacsUrl", map.get("PACS_URL"), "-", false); //影像链接
			Utils.checkAndPutToMap(exam, "examClassName", map.get("EXAM_CLASS_NAME"), "-", false); //病理类型名称
			for (Map<String, String> type : types) {
				if (map.get("EXAM_CLASS_NAME").equals(type.get("name"))) {
					exam.put("examTypeCode", type.get("id"));
				}
			}
			exams.add(exam);
		}
	}

	@Override
	public List<Map<String, String>> getPathologyReportTypes(String patientId, String outPatientId, String visitType) {

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Map<String, String> items = new HashMap<String, String>();

		if (StringUtils.isNotBlank(patientId)) {
			getPathologyTypesByPat(patientId, visitType, items);
		}
		if (StringUtils.isNotBlank(outPatientId)) {
			String[] pats = outPatientId.split(",");
			for (int i = 0; i < pats.length; i++) {
				if (StringUtils.isNotBlank(pats[i])) {
					String[] pat = pats[i].split("\\|");
					getPathologyTypesByPat(pat[1], pat[0], items);
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

		return list;
	}

	@Override
	public void getPathologyReportsCount(String patientId, Map<String, Object> resultMap, String outPatientId,
			String visitType) {

		int num = 0;
		if (StringUtils.isNotBlank(patientId)) {
			num = num + getPathologyTypesByPat(patientId, visitType);
		}
		if (StringUtils.isNotBlank(outPatientId)) {
			String[] pats = outPatientId.split(",");
			for (int i = 0; i < pats.length; i++) {
				if (StringUtils.isNotBlank(pats[i])) {
					String[] pat = pats[i].split("\\|");
					num = num + getPathologyTypesByPat(pat[1], pat[0]);
				}
			}
		}
		resultMap.put("num", num);
	}

	/**
	 * @param patId    患者编号
	 * @param pageNo   数量
	 * @param pageSize 数据集合
	 * @return 分页对象
	 * @Description 方法描述: 根据患者ID查询所有的 病理报告的类型
	 */
	public void getPathologyTypesByPat(String patientId, String visitType, Map<String, String> items) {
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		//病理报告权限筛选
		String usercode = SecurityUtils.getCurrentUserName(); //在线用户
		Map<String, Object> power = powerService.getPowerConfigByPathology(usercode);
		if ("false".equals(power.get("isAll"))) {
			List<String> typeList = (List<String>) power.get("power");
			String typeStr = StringUtils.join(typeList.toArray(), ",");
			filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.IN.getOperation(), typeStr));
		}
		filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.NOTIN.getOperation(),
				Config.getCIV_EXAM_VALUE().replace("/", ",")));
		filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
		Config.setExamStatusFilter(filters);
		List<Map<String, String>> outpage = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(),
				patientId, filters,  "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_CLASS_NAME");
		for (Map<String, String> map : outpage) {
			if (null == items.get(map.get("EXAM_CLASS_NAME"))) {
				items.put(map.get("EXAM_CLASS_NAME"), map.get("EXAM_CLASS_NAME"));
			}
		}
	}

	/**
	 * @param patId  患者编号
	 * @param pageNo 数量
	 * @return 分页对象
	 * @Description 方法描述: 根据患者ID查询总数
	 */
	public int getPathologyTypesByPat(String patientId, String visitType) {
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		//病理报告权限筛选
		String usercode = SecurityUtils.getCurrentUserName(); //在线用户
		Map<String, Object> power = powerService.getPowerConfigByPathology(usercode);
		if ("false".equals(power.get("isAll"))) {
			List<String> typeList = (List<String>) power.get("power");
			String typeStr = StringUtils.join(typeList.toArray(), ",");
			filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.IN.getOperation(), typeStr));
		}
		filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
		Config.setExamStatusFilter(filters);
		filters.add(new PropertyFilter("EXAM_CLASS_CODE", "STRING", MatchType.NOTIN.getOperation(),
				Config.getCIV_EXAM_VALUE().replace("/", ",")));
		List<Map<String, String>> outpage = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_EXAM_REPORT.getCode(),
				patientId, filters,  "EXAM_ITEM_NAME", "REPORT_TIME", "EXAM_CLASS_NAME");
		return outpage.size();
	}

}
