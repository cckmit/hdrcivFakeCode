package 逻辑;



@Service
public class AllergyService  {

	/**
	 *  某次就诊的过敏记录
	 * @param patientId 患者id
	 * @param visitId 就诊次数标识
	 * @param visitType 就诊类型
	 * @param orderBy 排序字段
	 * @param orderDir 排序规则
	 * @param pageNo 页码
	 * @param pageSize 每页大小
	 * @param filterString  过滤条件
	 */

	public void getAllergyList(String patientId, String visitId, String visitType, String orderBy,
			String orderDir, int pageNo, int pageSize, String filterString) {

	/*	如果 orderBy, orderDir 为空，默认设置排序字段为 RECORDTIME ，排序规则为 desc*/

	//过滤条件

	/*	如果 visitType = "OUTPV" ，增设查询条件 "VISIT_TYPE_CODE" = 01 ，
		否则 增设查询条件  "VISIT_TYPE_CODE" = 02 ；

		增设查询条件 "VISIT_ID" = visitId ;
	*/

	/*	如果过滤参数 filterString 不为空，将其转化为查询条件；*/


	/*
		根据 patientId ，结合所有查询条件从 HDR_ALLERGY 表中查出字段 {
			"ALLERGY_CATEGORY_NAME", "ALLERGEN", "ALLERGEN_NAME", "ALLERGY_REASON", "ALLERGY_REASON_NAME",
					"ALLERGY_REACTION", "ALLERGY_SEVERITY", "ALLERGY_SEVERITY_NAME", "OPERATOR_NURSE",
					"OPERATOR_NURSE_NAME", "OPERATE_NURSE_TIME", "RECORD_PERSON", "RECORD_PERSON_NAME", "RECORDTIME",
					"ALLERGY_NAME"
		}
	*/

		//字段映射
		/*
		如果Hbase中 "ALLERGY_REASON" 为空，从 "ALLERGY_REASON_NAME" 中取值；
		如果Hbase中 "ALLERGY_SEVERITY" 为空，从 "ALLERGY_SEVERITY_NAME" 中取值；
		如果Hbase中 "ALLERGEN" 为空，从 "ALLERGEN_NAME" 或"ALLERGY_NAME"取值；
		如果Hbase中 "RECORD_PERSON" 为空，从 "RECORD_PERSON_NAME"取值；
		如果Hbase中 "OPERATOR_NURSE" 为空，从 "OPERATOR_NURSE_NAME"取值；
		*/


	}

	@Override
	public Page<Map<String, String>> getAllergys(String patientId, String outPatientId, String orderBy,
			String orderDir, int pageNo, int pageSize, String filterString) {
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
			page.setOrderBy("RECORDTIME");
			page.setOrderDir("desc");
		} else {
			page.setOrderBy(orderBy);
			page.setOrderDir(orderDir);
		}
		//过滤条件
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		//附加条件
		if (StringUtils.isNotBlank(filterString)) {
			//设置条件 支持多条件
			CivUtils.strToFilter(filterString, filters, ";");
		}
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		if (StringUtils.isNotBlank(patientId)) {
			List<Map<String, String>> list1 = hbaseDao.findConditionByPatient("HDR_ALLERGY", patientId, filters,
					"ALLERGY_CATEGORY_NAME", "ALLERGEN", "ALLERGEN_NAME", "ALLERGY_REASON", "ALLERGY_REASON_NAME",
					"ALLERGY_REACTION", "ALLERGY_SEVERITY", "ALLERGY_SEVERITY_NAME", "OPERATOR_NURSE",
					"OPERATOR_NURSE_NAME", "OPERATE_NURSE_TIME", "RECORD_PERSON", "RECORD_PERSON_NAME", "RECORDTIME",
					"ALLERGY_NAME");
			list.addAll(list1);
		}
		//关联患者号
        if (StringUtils.isNotBlank(outPatientId)) {
            String[] ids = outPatientId.split(",");
            for (String pid : ids) {
                String[] pid_vid = pid.split("\\|");
                if (!patientId.equals(pid_vid[1])) {
                    List<Map<String, String>> list2 = hbaseDao.findConditionByPatient("HDR_ALLERGY", pid_vid[1], filters,
                            "ALLERGY_CATEGORY_NAME", "ALLERGEN", "ALLERGEN_NAME", "ALLERGY_REASON", "ALLERGY_REASON_NAME",
                            "ALLERGY_REACTION", "ALLERGY_SEVERITY", "ALLERGY_SEVERITY_NAME", "OPERATOR_NURSE",
                            "OPERATOR_NURSE_NAME", "OPERATE_NURSE_TIME", "RECORD_PERSON", "RECORD_PERSON_NAME",
                            "RECORDTIME", "ALLERGY_NAME");
                    list.addAll(list2);
                }
            }
        }
		//排序
		Utils.sortListByDate(list, page.getOrderBy(), page.getOrderDir());
		if (pageable) {
			//分页
			ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(list, page.getPageNo(),
					page.getPageSize());
			page.setTotalCount(listPage.getTotalCount());
			page.setResult(listPage.getPagedList());
		} else {
			page.setTotalCount(list.size());
			page.setResult(list);
		}
		//字段映射
		List<Map<String, String>> allergys = new ArrayList<Map<String, String>>();
		for (Map<String, String> map : page) {
			Map<String, String> allergy = new HashMap<String, String>();
			//过敏原因字段处理
			if (StringUtils.isBlank(map.get("ALLERGY_REASON"))) {
				map.put("ALLERGY_REASON", map.get("ALLERGY_REASON_NAME"));
			}
			//过敏程度字段处理
			if (StringUtils.isBlank(map.get("ALLERGY_SEVERITY"))) {
				map.put("ALLERGY_SEVERITY", map.get("ALLERGY_SEVERITY_NAME"));
			}
			//过敏源字段处理
			if (StringUtils.isBlank(map.get("ALLERGEN"))) {
				if (StringUtils.isNotBlank(map.get("ALLERGEN_NAME"))) {
					map.put("ALLERGEN", map.get("ALLERGEN_NAME"));
				} else {
					map.put("ALLERGEN", map.get("ALLERGY_NAME"));
				}
			}
			//记录人字段处理
			if (StringUtils.isBlank(map.get("RECORD_PERSON"))) {
				map.put("RECORD_PERSON", map.get("RECORD_PERSON_NAME"));
			}
			//处理护士字段处理
			if (StringUtils.isBlank(map.get("OPERATOR_NURSE"))) {
				map.put("OPERATOR_NURSE", map.get("OPERATOR_NURSE_NAME"));
			}
			ColumnUtil.convertMapping(allergy, map, "ALLERGY_CATEGORY_NAME", "ALLERGEN", "ALLERGY_REASON",
					"ALLERGY_REACTION", "ALLERGY_SEVERITY", "OPERATOR_NURSE", "OPERATE_NURSE_TIME", "RECORD_PERSON",
					"RECORDTIME");
			allergys.add(allergy);
		}
		page.setResult(allergys);
		return page;
	}

	@Override
	public long getAllergyCount(String patientId, String visitId, String visitType) {
		Page<Map<String, String>> page = getAllergyList(patientId, visitId, visitType, "", "", 0, 0, "");
		long allergyCount = page.getTotalCount();
		if (allergyCount < 0) {
			allergyCount = 0;
		}
		return allergyCount;
	}

	@Override
	public List<Map<String, String>> getAllergyTypes() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		String allergyTypeString = Config.getAllergyTypes();
		String[] types = allergyTypeString.split(";");
		for (String type : types) {
			Map<String, String> map = new HashMap<String, String>();
			String[] array = type.split("\\|");
			map.put("code", array[0]);
			map.put("name", array[1]);
			list.add(map);
		}
		return list;
	}

	@Override
	public List<Map<String, String>> getAllergySeverity() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		String allergyTypeString = Config.getAllergySeverity();
		String[] types = allergyTypeString.split(";");
		for (String type : types) {
			Map<String, String> map = new HashMap<String, String>();
			String[] array = type.split("\\|");
			map.put("code", array[1]);
			map.put("name", array[1]);
			list.add(map);
		}
		return list;
	}
}
