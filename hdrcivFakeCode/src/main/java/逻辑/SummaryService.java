package 逻辑;

import com.goodwill.core.orm.MatchType;
import com.goodwill.core.orm.Page;
import com.goodwill.core.orm.PropertyFilter;
import com.goodwill.core.utils.DateUtils;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.dao.HbaseDao;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.config.ConfigCache;
import com.goodwill.hdr.civ.enums.HdrTableEnum;
import com.goodwill.hdr.civ.utils.CivUtils;
import com.goodwill.hdr.civ.utils.ColumnUtil;
import com.goodwill.hdr.civ.utils.ListPage;
import com.goodwill.hdr.civ.utils.Utils;
import com.goodwill.hdr.civ.web.service.PowerService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

@Service
public class SummaryService  {

	Logger logger = LoggerFactory.getLogger(SummaryServiceImpl.class);
	//机构名称
	public static final String ORG_NAME = Config.getConfigValue("org_name");//Utils.convertToUtf8(PropertiesUtils.getPropertyValue("org.properties","org_name"));
	//机构代码
	public static final String ORG_OID =Config.getConfigValue("org_oid");// PropertiesUtils.getPropertyValue("hbase.properties", (("org_oid")));

	@Autowired
	private HbaseDao hbaseDao;
	@Autowired
	private PowerService powerService;

	@Override
	public List<String> getInpSummary(String patId, String visitId) {
		//封装数据
		List<String> result = new ArrayList<String>();
		List<Map<String, String>> fieldslist=Config.getSummaryFields();
		//页面查询条件
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		String[] fields=new String[fieldslist.size()];
		int count=0;
		for (Map<String, String> map : fieldslist) {
			if(!"-".equals(map.get("code"))){
				fields[count]=map.get("code");
			}else{
				fields[count]="";
			}
			count++;
		}
		List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(HdrTableEnum.HDR_INP_SUMMARY.getCode(),
				patId, visitId, filters, fields);
		//病案首页信息
		//未找到数据，中断执行
		Map<String, String> map =new HashMap<String, String>();
		if (list.size()> 0) {
			map = list.get(0);
		}
		//信息脱敏
		powerService.getInfoHidden(list);
		for (int i = 0; i < fieldslist.size(); i++) {
			Map<String, String> field=fieldslist.get(i);
			if("ORG_NAME".equals(field.get("code"))){
				result.add(field.get("name"));
				result.add(ORG_NAME);
				continue;
			}
			if("ORG_OID".equals(field.get("code"))){
				result.add(field.get("name"));
				result.add(ORG_OID);
				continue;
			}
			if("-".equals(field.get("code"))){
				result.add("-");
				result.add("");
				continue;
			}
			if("IN_HOSPITAL_DAYS".equals(field.get("code"))){
				String days="";
				if (StringUtils.isNotBlank(map.get(field.get("code")))) {
					days=map.get(field.get("code"));
				}else{
					String dischargeTime = map.get("DISCHARGE_TIME");
					if (StringUtils.isBlank(dischargeTime)) {
						dischargeTime = DateUtils.getNowDateTime();
					}
					if(StringUtils.isNotBlank(map.get("ADMISSION_TIME"))){
						days = DateUtils.calcInpDaysWithoutLast(DateUtils.convertStringToDate(dischargeTime),
								DateUtils.convertStringToDate(map.get("ADMISSION_TIME")))+"";
					}
				}
				result.add(field.get("name"));
				result.add(days);
				continue;
			}
			if("AUTOPSY_INDICATOR".equals(field.get("code"))){
				result.add(field.get("name"));
				String autopsy = map.get("AUTOPSY_INDICATOR");
				if ("true".equals(autopsy)) {
					result.add("是");
				} else {
					result.add("否");
				}
				continue;
			}
			result.add(field.get("name"));
			result.add(StringUtils.isNotBlank(map.get(field.get("code")))?map.get(field.get("code")):"");
		}
		return result;
	}

	@Override
	public List<String> getInpSummaryInfo(String patId, String visitId) {
		//封装数据
		List<String> result = new ArrayList<String>();
		List<Map<String, String>> fieldslist=Config.getSummaryInfoFields();
		//页面查询条件
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		String[] fields=new String[fieldslist.size()];
		int count=0;
		for (Map<String, String> map : fieldslist) {
			if(!"-".equals(map.get("code"))){
				fields[count]=map.get("code");
			}else{
				fields[count]="";
			}
			count++;
		}
		List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(HdrTableEnum.HDR_INP_SUMMARY.getCode(),
				patId, visitId, filters, fields);
		//病案首页信息
		//未找到数据，中断执行
		Map<String, String> map =new HashMap<String, String>();
		if (list.size()> 0) {
			map = list.get(0);
		}
		//新增脱敏
		List<Map<String, String>> listTemp =  new ArrayList<Map<String, String>>();
		listTemp.add(map);
		powerService.getInfoHidden(listTemp);
		for (int i = 0; i < fieldslist.size(); i++) {
			Map<String, String> field=fieldslist.get(i);
			if("AUTOPSY_INDICATOR".equals(field.get("code"))){
				result.add(field.get("name"));
				String autopsy = map.get("AUTOPSY_INDICATOR");
				if ("true".equals(autopsy)) {
					result.add("是");
				} else {
					result.add("否");
				}
				continue;
			}
			result.add(field.get("name"));
			result.add(StringUtils.isNotBlank(map.get(field.get("code")))?map.get(field.get("code")):"");
		}
		return result;
	}

	@Override
	public Map<String, Object> getInpSummaryDiag(String patId, String visitId) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(
				HdrTableEnum.HDR_INP_SUMMARY_DIAG.getCode(), patId, visitId, new ArrayList<PropertyFilter>(),
				new String[] { "DIAGNOSIS_TYPE_NAME", "DIAGNOSIS_NUM", "DIAGNOSIS_SUB_NUM", "DIAGNOSIS_CODE",
						"DIAGNOSIS_NAME", "DIAGNOSIS_TIME", "DIAGNOSIS_DOCTOR_NAME", "DIAGNOSIS_DESC",
						"DIAGNOSIS_PART", "CATALOG_TIME", "DIAGNOSIS_DEPT_NAME", "TREAT_RESULT_NAME", "TREAT_DAYS",
						"ADM_CONDITION_NAME" });
		//首页诊断
		List<Map<String, String>> diags = new ArrayList<Map<String, String>>();
		//未找到，中断执行
		if (list.size() == 0) {
			result.put("diags", diags);
			return result;
		}
		List<Map<String, String>> listSort = Utils.sortList(list, "DIAGNOSIS_NUM", "asc");
		for (Map<String, String> map : listSort) {
			Map<String, String> diag = new HashMap<String, String>();
			//是否主诊断
			if ("1".equals(map.get("DIAGNOSIS_NUM"))) {
				diag.put("mainFlag", "是");
			} else {
				diag.put("mainFlag", "否");
			}
			//诊断时间
			Utils.checkAndPutToMap(diag, "diagnosisTime", Utils.getDate("yyyy-MM-dd", map.get("DIAGNOSIS_TIME")), "-",
					false);
			//入院病情
			Utils.checkAndPutToMap(diag, "admConditionName", map.get("ADM_CONDITION_NAME"), "无", false);
			//字段映射
			ColumnUtil.convertMapping(diag, map, new String[] { "DIAGNOSIS_TYPE_NAME", "DIAGNOSIS_NAME",
					"DIAGNOSIS_CODE", "TREAT_RESULT_NAME" });

			diags.add(diag);
		}
		result.put("diags", diags);

		return result;
	}

	@Override
	public Map<String, Object> getInpSummaryOperation(String patId, String visitId) {
		//封装数据
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(
				HdrTableEnum.HDR_INP_SUMMARY_OPER.getCode(), patId, visitId, new ArrayList<PropertyFilter>(),
				new String[] { "DIAGNOSIS_CODE", "OPER_APPLY_NO", "OPER_NO", "OPERATION_NAME", "OPERATION_DESC",
						"OPERATION_DATE", "CATALOG_TIME", "OPERATION_GRADE_NAME", "SURGEN_CODE", "SURGEN_NAME",
						"FIRST_ASSISTANT_NAME", "SECOND_ASSISTANT_NAME", "ANESTHESIA_METHOD_NAME",
						"ANESTHESIA_DOCTOR_NAME", "ASA_GRADE_NAME", "WOUND_GRADE_NAME", "HEALING_GRADE_NAME",
						"OPERATION_CODE" });
		//首页手术
		List<Map<String, String>> operations = new ArrayList<Map<String, String>>();
		//未找到，中断执行
		if (list.size() == 0) {
			result.put("operations", operations);
			return result;
		}
		List<Map<String, String>> listSort = Utils.sortList(list, "OPER_NO", "asc");
		for (Map<String, String> map : listSort) {
			Map<String, String> operation = new HashMap<String, String>();
			Utils.checkAndPutToMap(operation, "operationDate", Utils.getDate("yyyy-MM-dd", map.get("OPERATION_DATE")),
					"-", false); //手术日期
			//拼接  切口等级/愈合等级
			String wound = map.get("WOUND_GRADE_NAME"); //切口等级
			String heal = map.get("HEALING_GRADE_NAME"); //愈合等级
			if (StringUtils.isNotBlank(wound) && StringUtils.isNotBlank(heal)) {
				operation.put("woundHeal", wound + "/" + heal);
			} else if (StringUtils.isNotBlank(wound) && StringUtils.isBlank(heal)) {
				operation.put("woundHeal", wound + "/");
			} else if (StringUtils.isBlank(wound) && StringUtils.isNotBlank(heal)) {
				operation.put("woundHeal", "/" + heal);
			} else {
				operation.put("woundHeal", "-");
			}
			//字段映射
			ColumnUtil.convertMapping(operation, map, new String[] { "OPERATION_CODE", "OPERATION_GRADE_NAME",
					"OPERATION_NAME", "SURGEN_NAME", "FIRST_ASSISTANT_NAME", "SECOND_ASSISTANT_NAME",
					"ANESTHESIA_METHOD_NAME", "ANESTHESIA_DOCTOR_NAME" });
			operations.add(operation);
		}
		result.put("operations", operations);
		return result;
	}

	@Override
	public List<Map<String, String>> getOutSummary(String patId, String visitId) {
		//GHHX 泰兴 挂号序号 字段
		List<Map<String, String>> outVisits = hbaseDao.findConditionByPatientVisitId(
				HdrTableEnum.HDR_OUT_VISIT.getCode(), patId, visitId, new ArrayList<PropertyFilter>(), new String[] {
						"VISIT_ID", "REG_CATEGORY_NAME", "REG_TYPE_NAME", "VISIT_DEPT_NAME", "VISIT_DOCTOR_NAME",
						"REGISTING_TIME", "VISIT_TIME", "EMERGENCY_VISIT_IND", "FIRSTV_INDICATOR", "PERSON_NAME",
						"DATE_OF_BIRTH", "ID_CARD_NO", "SEX_NAME", "REG_CATEGORY_NAME", "CHARGE_TYPE_NAME",
						"SEPARATE_TIME", "VISIT_CONSALT_ROOM", "EMERGENCY_VISIT_IND", "MARITAL_STATUS_NAME",
						"OCCUPATION_NAME", "MAILING_ADDRESS", "PHONE_NUMBER", "AGE_VALUE","GHHX" });
		return outVisits;
	}

	@Override
	public List<Map<String, String>> getOutSummaryDiag(String patId, String visitId) {
		List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(
				HdrTableEnum.HDR_OUT_VISIT_DIAG.getCode(), patId, visitId, new ArrayList<PropertyFilter>(),
				new String[] { "DIAGNOSIS_NUM", "DIAGNOSIS_SUB_NUM", "DIAGNOSIS_CODE", "DIAGNOSIS_NAME",
						"DIAGNOSIS_TIME", "DIAGNOSIS_DOCTOR_NAME", "DIAGNOSIS_DESC", "DIAGNOSIS_PART", "ICD_CODE" });
		List<Map<String, String>> listSort = Utils.sortList(list, "DIAGNOSIS_NUM", "asc");
		return listSort;
	}

	/**
	 * 费用明细table配置
	 * @param visitType 就诊类型
	 */
	@Override
	public void getPatientFeeTable(String visitType){
	/*	根据 visitType 从mysql的civ_config表中读取配置，
		住院读取 FEE_TABLE_IN_CHARGE_CONFIG 配置项
		门诊读取 FEE_TABLE_OUT_CHARGE_CONFIG 配置项
	*/
	}

	/**
	 * 费用明细类型
	 * @param patientId
	 * @param visitId
	 * @param visitType
	 * @return
	 */
	@Override
	public Page<Map<String, String>> getPatientInpSummaryFeeTypes(String patientId, String visitId, String visitType,int pageNo,int pageSize) {
		if(pageNo == 0){
			pageNo = 1;
		}
		if(pageSize == 0){
			pageSize = 10;
		}
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(pageNo);
		page.setPageSize(pageSize);
		String tableName = HdrTableEnum.HDR_IN_CHARGE.getCode();
		if("INPV".equals(visitType)){
			visitType = "02";
		}else if("OUTPV".equals(visitType)){
			visitType = "01";
			tableName = HdrTableEnum.HDR_OUT_CHARGE.getCode();
		}
		//过滤条件
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		if(StringUtils.isNotBlank(visitType)){
			filters.add(new PropertyFilter("VISIT_TYPE_CODE","STRING",MatchType.EQ.getOperation(),visitType));
		}
		List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(tableName,
				patientId, visitId, filters, new String[]{"BILL_ITEM_CODE","BILL_ITEM_NAME"});
		List<Map<String,String>> typeList = new ArrayList<Map<String, String>>();
		Map<String, String> mapAll = new HashMap<String, String>();
		mapAll.put("type_code","all");
		mapAll.put("type_name","全部");
		typeList.add(mapAll);
		for1:
		for (Map<String, String> map : list) {
			for2:
			for (Map<String, String> mapTemp : typeList) {
				//CHARGE_CLASS_CODE
				if (null != map.get("BILL_ITEM_NAME") && map.get("BILL_ITEM_NAME").equals(mapTemp.get("type_name"))) {
					continue for1;
				}
			}
			Map<String, String> mapRes = new HashMap<String, String>();
			mapRes.put("type_code",map.get("BILL_ITEM_CODE"));
			mapRes.put("type_name",map.get("BILL_ITEM_NAME"));
			typeList.add(mapRes);
		}
		page.setTotalCount(typeList.size());
		ListPage<Map<String, String>> listPage = new ListPage<Map<String, String>>(typeList,pageNo,pageSize);
		page.setResult(listPage.getPagedList());
		return page;
	}

	/**
	 *
	 * 费用明细数据
	 * @param patientId  患者id
	 * @param visitId 就诊次数
	 * @param visitType 就诊类型
	 * @param feeType 费用类型
	 * @param pageNo 当前页码
	 * @param pageSize 每页大小
	 */
	public void getPatientInpSummaryFeeData(String patientId, String visitId,
											String visitType,String feeType,
											int pageNo,int pageSize){

	/*使用用visit_type，调用 getPatientFeeTable 获取表头配置*/
		getPatientFeeTable(visitType);

	/*	住院患者查询对应表名 HDR_IN_CHARGE ；
		门诊患者查询对应表名 HDR_OUT_CHARGE ；
	*/

	/*	如果 visitType 不为空，那么增设查询条件{
			"VISIT_TYPE_CODE" = visitType
		}
	*/



	/*	如果 feeType 不为空，且 不等于"all" ,那么增设查询条件{
			"BILL_ITEM_CODE" = feeType
		}
	*/

	/*	根据 patientId、visitId ，结合所有查询条件，查询对应表名，查出所有字段。*/

	/*根据配置项，进行值映射，并返回给上一级。如果 "hidden" 为 true ，则不进行映射*/

	}

	@Override
	public Map<String, Object> getInpSummaryFee(String patId, String visitId) {
		Map<String, Object> result = new HashMap<String, Object>();
		String[] columns = new String[] { "COUNT_CHARGE_FEE", "CHARGE_FEE1", "CHARGE_FEE2",
				"CHARGE_FEE3", "CHARGE_FEE4", "CHARGE_FEE5", "CHARGE_FEE6", "CHARGE_FEE7", "CHARGE_FEE8",
				"CHARGE_FEE9", "CHARGE_FEE10", "CHARGE_FEE11", "CHARGE_FEE12", "CHARGE_FEE13", "CHARGE_FEE14",
				"CHARGE_FEE15", "CHARGE_FEE16", "CHARGE_FEE17", "CHARGE_FEE18", "CHARGE_FEE19", "CHARGE_FEE20",
				"CHARGE_FEE21", "CHARGE_FEE22", "CHARGE_FEE23", "CHARGE_FEE24", "CHARGE_FEE25", "CHARGE_FEE26",
				"CHARGE_FEE27", "CHARGE_FEE28", "CHARGE_FEE29", "CHARGE_FEE30", "CHARGE_FEE31", "CHARGE_FEE32",
				"CHARGE_FEE33", "CHARGE_FEE34", "CHARGE_FEE35", "CHARGE_FEE36", "CHARGE_FEE37", "CHARGE_FEE38",
				"CHARGE_FEE39", "CHARGE_FEE40"};
		List<String>  columnsList = new ArrayList<String>(Arrays.asList(columns));
		//获取配置文件的字段
		String  columnConfig = Config.getVISIT_SUMMARY_FEE_COLUMN();
		List<Map<String,String>> columnMap = new ArrayList<Map<String,String>>();
		if (StringUtils.isNotBlank(columnConfig)) {
			String columnes[] = columnConfig.split(";");
			for (String column : columnes) {
				// info[0]大类  info[1] 字段名  info[2] 字段名称
				String info[] = column.split(",");
				Map<String,String> map = new HashMap<String, String>();
				map.put("main_class",info[0]);
				map.put("field_code",info[1]);
				map.put("field_name",info[2]);
				columnMap.add(map);
				//添加到查询字段里
				columnsList.add(info[1]);
			}
		}
		//过滤条件
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		//查询病案首页 仅取费用相关字段
		List<Map<String, String>> list = hbaseDao.findConditionByPatientVisitId(HdrTableEnum.HDR_INP_SUMMARY.getCode(),
				patId, visitId, filters, columnsList.toArray(new String[]{}));
		//存储分类后费用信息
		List<Map<String, Object>> feeList = new ArrayList<Map<String, Object>>();
		//未找到，终止执行
		if (list.size() == 0) {
			result.put("total", "0.00"); //总费用
			result.put("selfExpense", "0.00"); //自付费用
			result.put("data", feeList);
			return result;
		}
		Map<String, String> map = list.get(0);
		//综合医疗服务类
		Map<String, Object> map1 = new HashMap<String, Object>();
		List<Map<String, Object>> map1List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map11 = new HashMap<String, Object>();
		map11.put("name", "一般医疗服务费");
		map11.put("cost", calCost(map.get("CHARGE_FEE2"), map.get("CHARGE_FEE27"), map.get("CHARGE_FEE28")));
		map1List.add(map11);
		Map<String, Object> map12 = new HashMap<String, Object>();
		map12.put("name", "一般治疗操作费");
		map12.put(
				"cost",
				calCost(map.get("CHARGE_FEE9"), map.get("CHARGE_FEE22"), map.get("CHARGE_FEE14"),
						map.get("CHARGE_FEE29")));
		map1List.add(map12);
		Map<String, Object> map13 = new HashMap<String, Object>();
		map13.put("name", "护理费");
		map13.put("cost", calCost(map.get("CHARGE_FEE15")));
		map1List.add(map13);
		Map<String, Object> map14 = new HashMap<String, Object>();
		map14.put("name", "其他费用");
		map14.put("cost", calCost(map.get("CHARGE_FEE39")));
		map1List.add(map14);
		map1.put("name", "综合医疗服务类");
		map1.put("综合医疗服务类", "综合医疗服务类");
		map1.put("data", map1List);
		feeList.add(map1);

		//诊断类
		Map<String, Object> map2 = new HashMap<String, Object>();
		List<Map<String, Object>> map2List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map21 = new HashMap<String, Object>();
		map21.put("name", "临床诊断项目费");
		map21.put("cost", calCost(map.get("CHARGE_FEE3")));
		map2List.add(map21);
		Map<String, Object> map22 = new HashMap<String, Object>();
		map22.put("name", "影像学诊断费");
		map22.put("cost", calCost(map.get("CHARGE_FEE16"), map.get("CHARGE_FEE18"), map.get("CHARGE_FEE19")));
		map2List.add(map22);
		Map<String, Object> map23 = new HashMap<String, Object>();
		map23.put("name", "实验室诊断费");
		map23.put("cost", calCost(map.get("CHARGE_FEE20")));
		map2List.add(map23);
		Map<String, Object> map24 = new HashMap<String, Object>();
		map24.put("name", "病理诊断费");
		map24.put("cost", calCost(map.get("CHARGE_FEE21")));
		map2List.add(map24);
		map2.put("name", "诊断类");
		map2.put("诊断类", "诊断类");
		map2.put("data", map2List);
		feeList.add(map2);

		//治疗类
		Map<String, Object> map3 = new HashMap<String, Object>();
		List<Map<String, Object>> map3List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map31 = new HashMap<String, Object>();
		map31.put("name", "临床物理治疗费");
		map31.put("cost", calCost(map.get("CHARGE_FEE4"), map.get("CHARGE_FEE17")));
		map3List.add(map31);
		Map<String, Object> map32 = new HashMap<String, Object>();
		map32.put("name", "非手术治疗项目费");
		map32.put("cost", calCost(map.get("CHARGE_FEE6"), map.get("CHARGE_FEE10")));
		map3List.add(map32);
		Map<String, Object> map33 = new HashMap<String, Object>();
		map33.put("name", "手术费");
		map33.put("cost", calCost(map.get("CHARGE_FEE5"), map.get("CHARGE_FEE11"), map.get("CHARGE_FEE13")));
		map3List.add(map33);
		Map<String, Object> map34 = new HashMap<String, Object>();
		map34.put("name", "麻醉费");
		map34.put("cost", calCost(map.get("CHARGE_FEE12")));
		map3List.add(map34);
		map3.put("name", "治疗类");
		map3.put("治疗类", "治疗类");
		map3.put("data", map3List);
		feeList.add(map3);

		//康复类
		Map<String, Object> map4 = new HashMap<String, Object>();
		List<Map<String, Object>> map4List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map41 = new HashMap<String, Object>();
		map41.put("name", "康复费");
		map41.put("cost", calCost(map.get("CHARGE_FEE7")));
		map4List.add(map41);
		map4.put("name", "康复类");
		map4.put("康复类", "康复类");
		map4.put("data", map4List);
		feeList.add(map4);

		//中医类
		Map<String, Object> map5 = new HashMap<String, Object>();
		List<Map<String, Object>> map5List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map51 = new HashMap<String, Object>();
		map51.put("name", "中医治疗费");
		map51.put("cost", calCost(map.get("CHARGE_FEE8")));
		map5List.add(map51);
		map5.put("name", "中医类");
		map5.put("中医类", "中医类");
		map5.put("data", map5List);
		feeList.add(map5);

		//西药类
		Map<String, Object> map6 = new HashMap<String, Object>();
		List<Map<String, Object>> map6List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map61 = new HashMap<String, Object>();
		map61.put("name", "西药费");
		map61.put("cost", calCost(map.get("CHARGE_FEE31")));
		map6List.add(map61);
		Map<String, Object> map62 = new HashMap<String, Object>();
		map62.put("name", "抗菌药物费用");
		map62.put("cost", calCost(map.get("CHARGE_FEE32")));
		map6List.add(map62);
		map6.put("name", "西药类");
		map6.put("西药类", "西药类");
		map6.put("data", map6List);
		feeList.add(map6);

		//中药类
		Map<String, Object> map7 = new HashMap<String, Object>();
		List<Map<String, Object>> map7List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map71 = new HashMap<String, Object>();
		map71.put("name", "中成药费");
		map71.put("cost", calCost(map.get("CHARGE_FEE37")));
		map7List.add(map71);
		Map<String, Object> map72 = new HashMap<String, Object>();
		map72.put("name", "中草药费");
		map72.put("cost", calCost(map.get("CHARGE_FEE38")));
		map7List.add(map72);
		map7.put("name", "中药类");
		map7.put("中药类", "中药类");
		map7.put("data", map7List);
		feeList.add(map7);

		//血液和血液制品类
		Map<String, Object> map8 = new HashMap<String, Object>();
		List<Map<String, Object>> map8List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map81 = new HashMap<String, Object>();
		map81.put("name", "血费");
		map81.put("cost", calCost(map.get("CHARGE_FEE30")));
		map8List.add(map81);
		Map<String, Object> map82 = new HashMap<String, Object>();
		map82.put("name", "白蛋白类制品费");
		map82.put("cost", calCost(map.get("CHARGE_FEE33")));
		map8List.add(map82);
		Map<String, Object> map83 = new HashMap<String, Object>();
		map83.put("name", "球蛋白类制品费");
		map83.put("cost", calCost(map.get("CHARGE_FEE34")));
		map8List.add(map83);
		Map<String, Object> map84 = new HashMap<String, Object>();
		map84.put("name", "凝血因子类制品费");
		map84.put("cost", calCost(map.get("CHARGE_FEE35")));
		map8List.add(map84);
		Map<String, Object> map85 = new HashMap<String, Object>();
		map85.put("name", "细胞因子类制品费");
		map85.put("cost", calCost(map.get("CHARGE_FEE36")));
		map8List.add(map85);
		map8.put("name", "血液和血液制品类");
		map8.put("血液和血液制品类", "血液和血液制品类");
		map8.put("data", map8List);
		feeList.add(map8);

		//耗材类
		Map<String, Object> map9 = new HashMap<String, Object>();
		List<Map<String, Object>> map9List = new ArrayList<Map<String, Object>>();
		Map<String, Object> map91 = new HashMap<String, Object>();
		map91.put("name", "治疗用一次性医用材料费");
		map91.put("cost", calCost(map.get("CHARGE_FEE23")));
		map9List.add(map91);
		Map<String, Object> map92 = new HashMap<String, Object>();
		map92.put("name", "手术用一次性医用材料费");
		map92.put("cost", calCost(map.get("CHARGE_FEE24"), map.get("CHARGE_FEE25")));
		map9List.add(map92);
		Map<String, Object> map93 = new HashMap<String, Object>();
		map93.put("name", "检查用一次性医用材料费");
		map93.put("cost", calCost(map.get("CHARGE_FEE26")));
		map9List.add(map93);
		map9.put("name", "耗材类");
		map9.put("耗材类", "耗材类");
		map9.put("data", map9List);
		feeList.add(map9);
		for (Map<String, String> mapTemp : columnMap) {
			String mainClass = mapTemp.get("main_class");
			for (Map<String, Object> feeMap : feeList) {
				String main_class = (String)feeMap.get(mainClass);
				if (mainClass.equals(main_class)) {
					Map<String, Object> mapConfig = new HashMap<String, Object>();
					mapConfig.put("name", mapTemp.get("field_name"));
					mapConfig.put("cost", calCost(map.get(mapTemp.get("field_code"))));
					List<Map<String, Object>> mapData  = (List<Map<String, Object>>)feeMap.get("data");
					mapData.add(mapConfig);
					break;
				}
			}
		}


		result.put("total", map.get("COUNT_CHARGE_FEE")); //总费用
		result.put("selfExpense", map.get("CHARGE_FEE1")); //自付费用
		result.put("data", feeList);
		return result;
	}

	/**
	 * @Description
	 * 方法描述: 计算费用，保留两位小数
	 * @return 返回类型： String
	 * @param feeStrings
	 * @return 费用字符串
	 */
	private String calCost(String... feeStrings) {
		double total = 0.00;
		if (null != feeStrings && feeStrings.length > 0) {
			for (String fee : feeStrings) {
				if(StringUtils.isNotBlank(fee)) {
					total = total + Double.parseDouble(fee);
				}
			}
		}
		//保留两位小数
		DecimalFormat df = new DecimalFormat("#0.00");
		return df.format(total);
	}

	@Override
	public Map<String, Object> getPatientOutpSummary(String patientId, String visitId) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, String>> outVisits = getOutSummary(patientId, visitId);
		//新增脱敏信息
		powerService.getInfoHidden(outVisits);
		//门诊信息
		Map<String, String> outMap = new HashMap<String, String>();
		//门诊诊断
		List<Map<String, String>> diags = new ArrayList<Map<String, String>>();
		//未找到，终止执行
		if (outVisits.size() == 0) {
			result.put("summary", outMap);
			result.put("diags", diags);
			return result;
		}
		Map<String, String> outVisit = outVisits.get(0);
		//根据出生日期 和 就诊时间计算年龄
		String ageValue = outVisit.get("AGE_VALUE");
		String birthDate = outVisit.get("DATE_OF_BIRTH");
		String visitTime = outVisit.get("VISIT_TIME");
		if (StringUtils.isNotBlank(ageValue)) {
			outMap.put("ageValue", ageValue);
		} else {
			if (StringUtils.isNotBlank(birthDate) && StringUtils.isNotBlank(visitTime)) {
				String age = CivUtils.calAge(visitTime, birthDate);
				outMap.put("ageValue", age);
			} else {
				outMap.put("ageValue", "-");
			}
		}
		Utils.checkAndPutToMap(outMap, "birthDate", Utils.getDate("yyyy年MM月dd日", birthDate), "-", false); //出生日期
		Utils.checkAndPutToMap(outMap, "visitTime", visitTime, "-", false); //就诊时间
		//急诊标识
		String emgFlag = outVisit.get("EMERGENCY_VISIT_IND");
		if ("true".equals(emgFlag)) {
			outMap.put("emergencyVisit", "是");
		} else {
			outMap.put("emergencyVisit", "否");
		}
		//初诊标识
		String firstV = outVisit.get("FIRSTV_INDICATOR");
		if ("true".equals(firstV)) {
			outMap.put("firstIndicator", "是");
		} else {
			outMap.put("firstIndicator", "否");
		}
		ColumnUtil.convertMapping(outMap, outVisit, new String[] { "MARITAL_STATUS_NAME", "OCCUPATION_NAME",
				"ID_CARD_NO", "MAILING_ADDRESS", "PHONE_NUMBER", "REG_CATEGORY_NAME", "VISIT_ID", "VISIT_DEPT_NAME",
				"VISIT_DOCTOR_NAME", "PERSON_NAME", "SEX_NAME","GHHX" });
		//查询患者表 补全门诊表中没有的患者信息
		Map<String, String> patientInfo = getPatientInfo(patientId, "01");
		List<Map<String,String>> patientInfoList = new ArrayList<Map<String, String>>();
		patientInfoList.add(patientInfo);
		powerService.getInfoHidden(patientInfoList);
		patientInfo = patientInfoList.get(0);
		ColumnUtil.convertMapping(outMap, patientInfo, new String[] { "NATIONALITY_NAME", "POSTCODE", "NEXT_OF_KIN",
				"RELATIONSHIP_NAME", "NEXT_OF_KIN_PHONE" });
		//新增脱敏信息
//		for(String key : outMap.keySet()){
//			if("personName".equals(key)){
//              outMap.put(key,powerService.getInfoHidden(outMap.get(key),"name"));
//			}
//			if("phoneNumber".equals(key)){
//				outMap.put(key,powerService.getInfoHidden(outMap.get(key),"phone"));
//			}
//			if("idCardNo".equals(key)){
//				outMap.put(key,powerService.getInfoHidden(outMap.get(key),"no"));
//			}
//			if("postcode".equals(key)){
//				outMap.put(key,powerService.getInfoHidden(outMap.get(key),"cardNo"));
//			}
//		}
		result.put("summary", outMap);

		//获取门诊诊断
		List<Map<String, String>> diagList = getOutSummaryDiag(patientId, visitId);
		for (Map<String, String> map : diagList) {
			Map<String, String> diag = new HashMap<String, String>();
			ColumnUtil.convertMapping(diag, map, new String[] { "DIAGNOSIS_CODE", "DIAGNOSIS_NAME", "DIAGNOSIS_NUM",
					"DIAGNOSIS_TIME", "DIAGNOSIS_DOCTOR_NAME" });
			diags.add(diag);
		}
		result.put("diags", diags);

		return result;
	}

	@Override
	public Map<String, String> getPatientInfo(String patientId, String visitType) {
		Map<String, String> infos = new HashMap<String, String>();
		infos.put("PATIENT_ID", patientId);
		List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
		filters.add(new PropertyFilter("VISIT_TYPE_CODE", "STRING", MatchType.EQ.getOperation(), visitType));
		//可能得到两条记录  门诊患者信息 和 住院患者信息
		List<Map<String, String>> indexs = hbaseDao.findConditionByPatient(HdrTableEnum.HDR_PATIENT.getCode(),
				patientId, filters, new String[] { "EID", "PERSON_NAME", "SEX_NAME", "DATE_OF_BIRTH", "INP_NO",
						"OUTP_NO", "IN_PATIENT_ID", "OUT_PATIENT_ID", "NATIONALITY_NAME", "POSTCODE", "NEXT_OF_KIN",
						"RELATIONSHIP_NAME", "NEXT_OF_KIN_PHONE" });
		//未找到患者，返回空的map
		if (indexs.size() == 0) {
			return infos;
		}
		//循环遍历患者信息  记录需要的字段
		for (Map<String, String> one : indexs) {
			Utils.checkAndPutToMap(infos, "PERSON_NAME", one.get("PERSON_NAME"), "-", false); //患者名
			Utils.checkAndPutToMap(infos, "SEX_NAME", one.get("SEX_NAME"), "-", false); //性别
			Utils.checkAndPutToMap(infos, "EID", one.get("EID"), "-", false); //主索引
			Utils.checkAndPutToMap(infos, "INP_NO", one.get("INP_NO"), "住院号未知", false); //住院号
			Utils.checkAndPutToMap(infos, "OUTP_NO", one.get("OUT_NO"), "门诊号未知", false); //门诊号
			Utils.checkAndPutToMap(infos, "IN_PATIENT_ID", one.get("IN_PATIENT_ID"), "-", false); //住院患者标识
			Utils.checkAndPutToMap(infos, "OUT_PATIENT_ID", one.get("OUT_PATIENT_ID"), "-", false); //门诊患者标识
			Utils.checkAndPutToMap(infos, "DATE_OF_BIRTH", one.get("DATE_OF_BIRTH"), "-", false); //出生日期
			Utils.checkAndPutToMap(infos, "NATIONALITY_NAME", one.get("NATIONALITY_NAME"), "-", false); //民族
			Utils.checkAndPutToMap(infos, "POSTCODE", one.get("POSTCODE"), "-", false); //邮编
			Utils.checkAndPutToMap(infos, "NEXT_OF_KIN", one.get("NEXT_OF_KIN"), "-", false); //联系人姓名
			Utils.checkAndPutToMap(infos, "RELATIONSHIP_NAME", one.get("RELATIONSHIP_NAME"), "-", false); //与患者关系
			Utils.checkAndPutToMap(infos, "NEXT_OF_KIN_PHONE", one.get("NEXT_OF_KIN_PHONE"), "-", false); //联系人电话
		}
		//处理出生日期
		String birthday = infos.get("DATE_OF_BIRTH");
		if (!"-".equals(birthday)) {
			infos.put("DATE_OF_BIRTH", Utils.getDate("yyyy-MM-dd", birthday));
		}
		return infos;
	}
}
