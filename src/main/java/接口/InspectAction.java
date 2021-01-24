package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.InspectReportService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

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

	@Autowired
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
	 * @Description
	 * 某份检验报告的详情  基本信息+检验细项列表
	 */
	public void getInspectDetails() {
		//报告号 显示标识 1:显示异常结果
		String reportNo = getParameter("reportNo");
		String show = getParameter("show");
		Map<String, Object> result = inspectReportService.getInspectReportDetails(patientId, reportNo, pageNo,
				pageSize, show);
		//响应
		renderJson(JsonUtil.getJSONString(result));
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
