package 接口;

import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.web.entity.CommonConfig;
import com.goodwill.hdr.civ.web.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public class CategoryAction extends CIVAction {

	private static final long serialVersionUID = 1L;

	@Autowired
	private PowerService powerService;
	@Autowired
	private PathologyReportService pathology;
	@Autowired
	private InspectReportService exam;
	@Autowired
	private CheckReportService check;
	@Autowired
	private OperService oper;
	@Autowired
	private DiagnoseService diag;
	@Autowired
	private MedicalRecordService medical;
	@Autowired
	private OrderService order;
	@Autowired
	private VisitService visitService;
	@Autowired
	private CurrentViewService cvService;
	@Autowired
	private NursingService nursingService;

	/**
	 * @Description 获得分类视图数量
	 */
	public void getModernNum() {
		String userCode = getParameter("userCode");
		String outPatientId = getParameter("outPatientId");
		String visitType = getParameter("visitType");

		List<Map<String, Object>> list = powerService.getPowerConfigByCategory(userCode);
		for (Map<String, Object> map : list) {
			if (StringUtils.isNotBlank(map.get("id").toString())) {
				String code = map.get("id").toString();
				if ("exam_module".equals(code)) {
					exam.getAllReportsCount(patientId, map, outPatientId, visitType);
				} else if ("pathology_module".equals(code)) {
					pathology.getPathologyReportsCount(patientId, map, outPatientId, visitType);
				} else if ("check_module".equals(code)) {
					check.getAllReportsCount(patientId, map, outPatientId, visitType);
				} else if ("oper_module".equals(code)) {
					oper.getOpersNum(patientId, visitType, map, outPatientId);
				} else if ("main_diag_module".equals(code)) {
					diag.getDiagsListNum(patientId, visitType, map);
				} else if ("record_module".equals(code)) {
					medical.getAllMRCount(patientId, visitType, map, outPatientId);
				} else if ("nurse_module".equals(code)) {
					//暂时在这里判断是否配置了url
					boolean isDistinguish = Config.getCIV_NURSE_URL_OUT_OR_IN();
					if (StringUtils.isNotBlank(CommonConfig.getURL("NURSE")) ||
							(isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("IN_NURSE"))) ||
							(isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("OUT_NURSE")))) {
						map.put("map", "");
					} else {
						map.put("num",
								nursingService.getNurseNum(patientId, visitId, visitType, outPatientId, "all", 1, 10));
					}
				} else if ("durg_orally_module".equals(code)) {
					order.getOrdersNum(patientId, map, "KF", outPatientId, visitType);
				} else if ("durg_vein_module".equals(code)) {
					order.getOrdersNum(patientId, map, "JM", outPatientId, visitType);
				} else if ("durg_qt_module".equals(code)) {
					order.getOrdersNum(patientId, map, "QT", outPatientId, visitType);
				} else if ("dialysis_module".equals(code)) {
					map.put("num", 0);
				}
			}
		}
		renderJson(JsonUtil.getJSONString(list));
	}

	/**
	 * @Description 获取最后一次就诊的现病史
	 */
	public void getHistory_Module() {
		String patientId = getParameter("patientId");
		String visitId = visitService.getFinalInVisit(patientId, "");
		Map<String, String> result = cvService.getCVNowHis(patientId, visitId);
		renderJson(JsonUtil.getJSONString(result));

	}
}
