package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.utils.CusInfoUtil;
import com.goodwill.hdr.civ.web.entity.LogRecordAnalysis;
import com.goodwill.hdr.civ.web.service.LogMonitorService;
import com.goodwill.hdr.civ.web.service.PowerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author guozhenlei
 * @Description 类描述：日志监控action
 * @Date 2019-06-12 11:37
 * @modify 修改记录：
 */
public class LogMonitorAction extends CIVAction {

    private static final long serialVersionUID = 1L;
    @Autowired
    private PowerService powerService;
    @Autowired
    private LogMonitorService logMonitorService;

    /**
     * @Description 各页面点击触发监控日志保存
     */
    public void insetMonitorLog() {
    	if(StringUtils.isNotBlank(getParameter("dept_code"))
    			&&StringUtils.isNotBlank(getParameter("dept_name"))){
    		LogRecordAnalysis logRecord = new LogRecordAnalysis();
    		logRecord.setDeptcode(getParameter("dept_code"));
    		logRecord.setDeptname(getParameter("dept_name"));
    		logRecord.setPagecode(getParameter("code"));
    		logRecord.setPagename(getParameter("name"));
    		//获取客户端ip
    		HttpServletRequest request = getRequest();
    		logRecord.setIp(CusInfoUtil.getRealIP(request));
    		logMonitorService.insertMonitorLog(logRecord);
    	}
    }

    /**
     * 获得监控日志查询条件：医生列表
     */
    public void getDoctorList() {
        String userName = getParameter("keyWord");
        String deptCode = getParameter("dept");
        String pageNo = getParameter("pageNo");
        String pageSize = getParameter("pageSize");
        Page<Map<String, String>> map = logMonitorService.getDoctorPage(userName, deptCode, Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获得当前权限视图
     */
    public void getPowerConfigByPage() {
        String userCode = getParameter("userCode");
        if (null == userCode) {
            userCode = "";
        }
        Map<String, String> map = powerService.getPowerConfigByPage(userCode);
        map = logMonitorService.handleView(map);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获取监控日志数据列表
     */
    public void getMonitorLog() {
        String deptCode = getParameter("dept");
        String deptName = getParameter("deptName");
        String userCode = getParameter("doctor");
        String userName = getParameter("userName");
        String visitPageCode = getParameter("page");
        String vistiPageName = getParameter("pageName");
        String beginDate = getParameter("start_time");
        String endDate = getParameter("end_time");
        String pageNoStr = getParameter("pageNo");
        int pageNo = pageNoStr.equals("0") ? 10 : Integer.valueOf(pageNoStr);
        String pageSizeStr = getParameter("pageSize");
        int pageSize = pageSizeStr.equals("0") ? 10 : Integer.valueOf(pageSizeStr);
        Page<Map<String, String>> map = logMonitorService.getMonitorLogPage(deptCode,  userCode,  visitPageCode,
                beginDate, endDate, pageNo, pageSize);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 获取所有的监控日志数据
     */
    public void getAllMonitorLog() {
        Page<Map<String, String>> map = logMonitorService.getAllMonitorLogPage();
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 根据不同条件进行分类获取监控日志数据
     * 条形图：按科室，按医生
     */
    public void getClassifyMonitorLog() {
        String flag = getParameter("id");  //分类 according_doctor  according_dept
        String beginDate = getParameter("start_time");
        String endDate = getParameter("end_time");
        String value = getParameter("detp");
        Map<String, Object> map = logMonitorService.getClassifyMonitorLog(value, beginDate, endDate, flag);
        renderJson(JsonUtil.getJSONString(map));
    }

    /**
     * 条形图：科室按时间统计，医生按时间统计
     */
    public void getClassifyByTimeMonitorLog() {
        String flag = getParameter("id"); // according_depttime  according_doctortime
        String dept = getParameter("detp");
        String doctor = getParameter("doctor");
        String days = getParameter("time");
        if (null==dept||dept.equals("")) {
            dept = "01";
        }
        if (null==days||days.equals("")) {//默认最近一个月
            days = "1";
        }
        if(flag.equals("according_doctortime")) {
            if (null == doctor || doctor.equals("")) {
                Map<String, Object>  mapEmpty = logMonitorService.getEmptyData(days);
                renderJson(JsonUtil.getJSONString(mapEmpty));
                return;
            }
        }
        Map<String, Object>  map = logMonitorService.getSortedByTimeMonitorLog(dept, doctor, days, flag);
        renderJson(JsonUtil.getJSONString(map));
    }


    /**
     * top 5 监控日志查询
     */
    public void getTopFiveMonitorLogData() {
        // id = top5_dept  科室top 5
        // id = top5_doctor   医生top5
        String flag = getParameter("id");
        String value = getParameter("code")==null?"":getParameter("code");
        String userCode = getParameter("userCode");
        List<Map<String, String>> map = logMonitorService.getTopFiveLogData(flag, value, userCode);
        renderJson(JsonUtil.getJSONString(map));
    }


    /**
     * 扇形图
     */
    public void getSectorData() {
        List<Map<String, String>> map = logMonitorService.getSectorData();
        renderJson(JsonUtil.getJSONString(map));
    }


}
