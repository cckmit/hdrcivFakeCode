package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.entity.CommonConfig;
import com.goodwill.hdr.civ.web.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhaowenkai
 * @Description 类描述：门诊就诊Action
 * @Date 2018年4月19日
 * @modify 修改记录：
 */
public class VisitAction extends CIVAction {

	private static final long serialVersionUID = 1L;

	@Autowired
	private VisitService visitService;
	@Autowired
	private SummaryService summaryService;
	@Autowired
	private OrderService orderService;
	@Autowired
	private InspectReportService inspectReportService;
	@Autowired
	private CheckReportService checkReportService;
	@Autowired
	private PathologyReportService pathologyReportService;
	@Autowired
	private MedicalRecordService medicalRecordService;
	@Autowired
	private OperService operService;
	@Autowired
	private CommonURLService commonURLService;
	@Autowired
	private AllergyService allergyService;
	@Autowired
	private NursingService nursingService;

	@Autowired
	private GlobalService globalService;

	/**
	 * @Description 获取患者就诊列表
	 */
	public void getPatientVisitsInfo() {
		//获取 患者编号 就诊类型 日期类型 科室  开始时间  结束时间
		String dateType = getParameter("visitDate");
		String visitDept = getParameter("visitDept");
		String startTime = getParameter("timeStart");
		String endTime = getParameter("timeEnd");
		//判断是统计科室还是就诊列表
		String showList = getParameter("showList");
		//门诊住院条件
		String deptType = getParameter("deptType");
		long  time = System.currentTimeMillis();
		Map<String, Object> result = visitService.getPatientVisitsInfo(patientId, visitType, startTime, endTime,
				dateType, visitDept, showList, deptType);
		System.out.println(System.currentTimeMillis()-time);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取患者病案首页
	 */
	public void getPatientInpSummary() {
		//获取 患者编号  就诊次数
		List<String> result = summaryService.getInpSummary(patientId, visitId);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取患者病案首页
	 */
	public void getPatientInpSummaryInfo() {
		//获取 患者编号  就诊次数
		List<String> result = summaryService.getInpSummaryInfo(patientId, visitId);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取病案首页诊断
	 */
	public void getPatientInpSummaryDiag() {
		Map<String, Object> result = summaryService.getInpSummaryDiag(patientId, visitId);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取病案首页手术
	 */
	public void getPatientInpSummaryOpera() {
		Map<String, Object> result = summaryService.getInpSummaryOperation(patientId, visitId);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取病案首页费用 分类统计
	 */
	public void getPatientInpSummaryFee() {
		Map<String, Object> result = summaryService.getInpSummaryFee(patientId, visitId);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * 费用明细类型
	 */
	public void getPatientInpSummaryFeeTypes() {
		Page<Map<String, String>> result = summaryService.getPatientInpSummaryFeeTypes(patientId, visitId, visitType,
				pageNo, pageSize);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * 费用明细table展示字段
	 */
	public void getPatientInpSummaryFeeTable() {
		String result = summaryService.getPatientFeeTable(visitType);
		//响应
		renderJson(result);
	}

	/**
	 * 费用明细数据
	 */
	public void getPatientInpSummaryFeeDetail() {
		String feeType = getParameter("feeType");
		Page<Map<String, String>> result = summaryService.getPatientInpSummaryFeeData(patientId, visitId, visitType,
				feeType, pageNo, pageSize);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取患者某次门诊的首页信息   就诊信息+门诊诊断
	 */
	public void getPatientOutpSummary() {
		Map<String, Object> result = summaryService.getPatientOutpSummary(patientId, visitId);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取患者末次就诊的信息
	 */
	public void getPatientFinalVisit() {
		String outPatientId = getParameter("outPatientId");
		Map<String, String> result = new HashMap<String, String>();
		Map<String, String> config = new HashMap<String, String>();
		if (StringUtils.isNotBlank(patientId)) {
			result = visitService.getCurrentPatientInfo(patientId, outPatientId, visitType);
			config = orderService.getPatLastInfoViewConfig();
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("viewConfig", config);
		map.put("data", result);
		//响应
		renderJson(JsonUtil.getJSONString(map));
	}
	/**
	 * @Description 通过pid，vid和visitType获取患者就诊信息
	 */
	public void getPatientVisit() {
		Map<String, String> result = new HashMap<String, String>();
		Map<String, String> config = new HashMap<String, String>();
		if (StringUtils.isNotBlank(patientId)) {
			result = visitService.getPatientInfo(patientId, visitId, visitType);
			config = orderService.getPatLastInfoViewConfig();
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("viewConfig", config);
		map.put("data", result);
		//响应
		renderJson(JsonUtil.getJSONString(map));
	}
	/**
	 * 获取末次就诊的科室信息
	 */
	public void getPatientFinalVisitDept() {
		String outPatientId = getParameter("outPatientId");
		Map<String, String> result = new HashMap<String, String>();
		if (StringUtils.isNotBlank(patientId)) {
			Map<String, String> data = visitService.getCurrentPatientInfo(patientId, outPatientId, visitType);
			result.put("deptCode", data.get("stay_dept_code"));
			result.put("deptName", data.get("stay_dept_name"));
		}
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取患者末次就诊的信息
	 */
	public void getPatientInFinalVisit() {
		String outPatientId = getParameter("outPatientId");
		Map<String, String> result = visitService.getPatientInFinalVisit(patientId, outPatientId, visitType);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 判断是否存在与当前患者编号不一致的患者标识，若存在，则返回
	 */
	public void getDifferencePids() {
		Map<String, Object> result = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(patientId))
			result = visitService.getOutOrInPatientId(patientId, visitType);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 某次就诊的各项数量   医嘱，检验报告，检查报告，病历文书，手术记录，护理记录，过敏记录
	 */
	public void getVisitItemCount() {
		Map<String, Object> result = new HashMap<String, Object>();
		//查询某次就诊的各项数量
		long orderCount = orderService.getOrderCount(patientId, visitId, visitType);
		long inspectCount = inspectReportService.getInspectCount(patientId, visitId, visitType);
		long checkCount = checkReportService.getCheckCount(patientId, visitId, visitType);
		long pathologyCount = pathologyReportService.getPathologyCount(patientId, visitId, visitType);
		long mrCount = medicalRecordService.getMRCount(patientId, visitId, visitType);
		long operCount = operService.getOperCount(patientId, visitId, visitType);
		long allergyCount = allergyService.getAllergyCount(patientId, visitId, visitType);
		result.put("orderCount", orderCount); //医嘱
		result.put("inspectCount", inspectCount); //检验报告
		result.put("pathologyCount", pathologyCount); //病理报告
		result.put("checkCount", checkCount); //检查报告
		result.put("mrCount", mrCount); //病历文书
		result.put("operCount", operCount); //手术记录
		result.put("allergyCount", allergyCount); //过敏记录
		//暂时在这里判断是否配置了url
        boolean isDistinguish = Config.getCIV_NURSE_URL_OUT_OR_IN();
        if (StringUtils.isNotBlank(CommonConfig.getURL("NURSE")) ||
                (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("IN_NURSE"))) ||
                (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("OUT_NURSE")))) {
            result.put("nurseCount", "");
        } else {
			result.put("nurseCount", nursingService.getNurseNum(patientId, visitId, visitType, "", "all", 1, 10));
		}
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 某次就诊的各类医嘱数量
	 */
	public void getVisitOrderCount() {
		Map<String, Object> result = orderService.getTypeOrderCount(patientId, visitId, visitType);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description 获取就诊列表上
	 */
	public void getHiddenVisitListVid() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("result", Config.getCIV_HIDDEN_VISITlIST_VISITID());
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * 获取末次就诊信息显示配置
	 */
	public void getPatLastInfoView() {
		Map<String, String> config = orderService.getPatLastInfoViewConfig();
		renderJson(JsonUtil.getJSONString(config));
	}

	/**
	 * 获取配置的医嘱闭环url
	 */
	public void getOclUrl() {
		//获取医嘱闭环配置url
		Map<String, String> map = commonURLService.getCommonUrl(patientId, visitId, "OCL",
				new HashMap<String, String>());
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * 就诊试图的科室筛选
	 */
	public void getShowConfig() {
		String deptShow = Config.getVisitDeptSelectConfig();
		Map<String, String> map = new HashMap<String, String>();
		map.put("config", deptShow);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * 默认打开的页面配置
	 */
	public void getDefaultPage() {
		String defaultPage = Config.getCIV_DEFAULT_PAGE();
		Map<String, String> map = new HashMap<String, String>();
		map.put("page", defaultPage);
		renderJson(JsonUtil.getJSONString(map));
	}

	/**
	 * 根据身份证号+患者姓名关联免登陆跳转地址
	 */
	public void getPatientinfo() {
		Map<String, String> result = new HashMap<String, String>();
		String id_crad_no = getParameter("idCardNo");
		String personName = getParameter("personName");
		String userName = getParameter("userName");
		result = visitService.getPatientinfo(id_crad_no, personName, userName);
		renderJson(JsonUtil.getJSONString(result));
	}



}
