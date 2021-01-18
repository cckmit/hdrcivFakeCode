package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.web.service.PathologyReportService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：病理Action
 * @author songhaibo
 * @Date 2020年8月7日
 */
public class PathologyAction extends CIVAction {

	private static final long serialVersionUID = 1L;
	@Autowired
	private PathologyReportService pathologyReportService;

	/**
	 * @Description
	 * 某次就诊的病理报告
	 */
	public void getPatientVisitPathology() {
		int pno = StringUtils.isBlank(getParameter("pno")) ? 0 : Integer.parseInt(getParameter("pno"));
		//参数 患者编号  就诊次  就诊类型
		Page<Map<String, String>> result = pathologyReportService.getPathologyReportList(patientId, visitId, visitType,
				"REPORT_TIME", "desc", "", "", pno, pageSize);
		//响应
		renderJson(JsonUtil.getJSONString(result));
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
