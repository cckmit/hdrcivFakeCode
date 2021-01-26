package 接口;

import 逻辑.InspectReportService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：检验Action
 * @author zhaowenkai
 * @Date 2018年6月14日
 * @modify
 * 修改记录：
 *
 */
public class InspectAction extends CIVAction {

	private static final long serialVersionUID = 1L;


	private InspectReportService inspectReportService;

	/**
	 * @Description
	 * 某次就诊的检验报告
	 */
	public void getPatientVisitInspects() {
		int pno = StringUtils.isBlank(getParameter("pno")) ? 0 : Integer.parseInt(getParameter("pno"));
		//患者编号  就诊次  就诊类型
		Page<Map<String, String>> result = inspectReportService.getInspectReportList(patientId, visitId, visitType,
				"REPORT_TIME", "desc", pageNo, pageSize);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 *
	 * 某份检验报告的详情  基本信息+检验细项列表
	 *
	 *
	 * @param patientId 患者编号
	 * @param reportNo 报告号
	 * @param pageNo 页码
	 * @param pageSize 分页单位
	 * @param show 显示标记   1:显示异常结果
	 */
	public void getInspectDetails(String patientId, String reportNo,
								  int pageNo, int pageSize,
								  String show) {

		/*将前端参数传递给逻辑层处理*/
		inspectReportService.getInspectReportDetails(patientId, reportNo, pageNo,
				pageSize, show);

	}

	/**
	 * @Description
	 * 某个检验细项的趋势图  如：白细胞变化趋势等
	 */
	public void getInspectItemLine() {
		String dateType = getParameter("dateType");
		String startDate = getParameter("startDate");
		String endDate = getParameter("endDate");
		String subItemCode = getParameter("code");
		String outPatientId = getParameter("outPatientId");
		String visitType = getParameter("visitType");
		Map<String, Object> result = inspectReportService.getInspectReportDetailsLine(patientId,visitType,outPatientId, dateType, startDate,
				endDate, subItemCode);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 当前视图 - 获取末次住院的异常检验结果
	 */
	public void getCVExLabResult() {
		List<Map<String, String>> result=new ArrayList<Map<String,String>>();
		String mainDiag = getParameter("mainDiag");
		String deptCode = getParameter("dept_code");
		if(StringUtils.isNotBlank(patientId))
			result = inspectReportService.getExLabResult(patientId, visitId, "INPV",mainDiag,deptCode);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获得所有检验报告
	 */
	public void getInspectReports() {
		String outPatientId = getParameter("outPatientId");
		String year = getParameter("year");
		String visitType = getParameter("visitType");

		List<Map<String, Object>> result = inspectReportService.getInspectReports(patientId,visitType, 1, 10000, "REPORT_TIME",
				"desc", outPatientId, year);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获得所有检验报告类型
	 */
	public void getReportsTypes() {
		String outPatientId = getParameter("outPatientId");
		String visitType = getParameter("visitType");

		List<Map<String, String>> result = inspectReportService.getReportsTypes(patientId, outPatientId,visitType);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

}
