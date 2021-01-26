package 接口;


import 逻辑.PathologyReportService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * 类描述：病理接口
 * @author 余涛
 * @date 2021年1月26日
 */
public class PathologyAction {


	private PathologyReportService pathologyReportService;


	/**
	 *
	 * 方法描述: 查询患者某次就诊的病理报告
	 *
	 * @param patientId 患者编号
	 * @param visitId 就诊次数
	 * @param visitType 就诊类型
	 * @param pno 页码
	 * @param pageSize 分页单位
	 *
	 */
	public void getPatientVisitPathology(String patientId, String visitId, String visitType,
										 int pno, int pageSize) {
		/*如果前端参数 "pno" 为空，则设为0；
		将参数传给逻辑层处理；
		*/
		 pathologyReportService.getPathologyReportList(patientId, visitId, visitType,
				"REPORT_TIME", "desc", "", "", pno, pageSize);

	}

	/**
	 * @Description
	 * 某份病理报告详情
	 */
	public void getPathologyDetails() {
		//病理报告主键
		String reportNo = getParameter("reportNo");
		Map<String, String> result = pathologyReportService.getPathologyReportDetails(patientId,visitId,reportNo);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获得患者所有的病理报告
	 */
	public void getPathologyReports() {
		String outPatientId = getParameter("outPatientId");
		String visitType = getParameter("visitType");
		String year = getParameter("year");
		String key = getParameter("key");
		String type = getParameter("type");
		String click_Type = getParameter("click_type");
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		result = pathologyReportService.getPathologyReports(patientId, 1, 2000, "REPORT_TIME", "desc", outPatientId,
				visitType, year, key, type, click_Type);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获得患者所有的病理报告类型
	 */
	public void getPathologyReportTypes() {
		String outPatientId = getParameter("outPatientId");
		String visitType = getParameter("visitType");
		String key = getParameter("key");
		List<Map<String, String>> result = pathologyReportService.getPathologyReportTypes(patientId, outPatientId,
				visitType);
		//响应
		renderJson(JsonUtil.getJSONString(result));
	}

}
