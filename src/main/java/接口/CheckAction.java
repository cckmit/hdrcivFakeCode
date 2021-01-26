package 接口;


import 逻辑.CheckReportService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：检查Action
 * @author zhaowenkai
 * @Date 2018年6月17日
 * @modify
 * 修改记录：
 *
 */
public class CheckAction extends CIVAction {

	private static final long serialVersionUID = 1L;
	@Autowired
	private CheckReportService checkReportService;

	/**
	 *
	 * 就诊视图，某次就诊的检查报告
	 * @param patientId 患者编号
	 * @param visitId 就诊次数
	 * @param visitType 就诊类型
	 * @param pageSize 分页单位	 *
	 */
	public void getPatientVisitChecks(String patientId, String visitId, String visitType, int pageSize) {
	 /* @param pno 页码
		从前端接收 pno 参数，如果 pno 为空，则设置为 0 */


	 /* @param   "REPORT_TIME" 对应 orderBy 参数，排序字段
	    @param   "desc" 对应 orderDir 参数，排序规则
		@param   "" 第一个空参数，对应 mainDiag 参数，诊断类型
		@param   "" 第二个空参数，对应 deptCode 参数，科室编号
		调用逻辑层 getCheckReportList	方法，查询结果，返回给前端 */
		 checkReportService.getCheckReportList(patientId, visitId, visitType,
				"REPORT_TIME", "desc","","", pno, pageSize);

	}

	/**
	 * @Description
	 * 当前视图 - 检查报告
	 */
	public void getCVChecks() {
		Page<Map<String, String>> result=new Page<Map<String,String>>();
		String mainDiag = getParameter("mainDiag");
        String deptCode =  getParameter("dept_code");
		if(StringUtils.isNotBlank(patientId))
			result = checkReportService.getCheckReportList(patientId, visitId, "INPV",
				"REPORT_TIME", "desc", mainDiag,deptCode,0, 0);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 某份检查报告详情
	 */
	public void getCheckDetails() {
		//检查报告主键
		String reportNo = getParameter("reportNo");
		Map<String, String> result = checkReportService.getCheckReportDetails(patientId,visitId,reportNo);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获得患者所有的检查报告
	 */
	public void getCheckReports() {
		String outPatientId = getParameter("outPatientId");
		String visitType = getParameter("visitType");
		String year = getParameter("year");
		String key = getParameter("key");
		String type = getParameter("type");
		String click_Type = getParameter("click_type");
		List<Map<String, Object>> result =new ArrayList<Map<String,Object>>();
		result = checkReportService.getCheckReports(patientId, 1, 2000, "REPORT_TIME","desc", outPatientId, visitType, year,key,type,click_Type);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获得患者所有的检查报告类型
	 */
	public void getAllReportTypes() {
		String outPatientId = getParameter("outPatientId");
		String visitType = getParameter("visitType");
		String key = getParameter("key");
		List<Map<String, String>> result = checkReportService.getAllReportTypes(patientId, outPatientId, visitType);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

}
