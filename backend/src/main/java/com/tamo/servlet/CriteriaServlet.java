package com.tamo.servlet;
import com.google.gson.*;
import com.tamo.service.*;
import com.tamo.model.*;
import com.tamo.util.JsonUtil;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
public class CriteriaServlet extends HttpServlet{
 private JsonDataService data;@Override public void init(){data=JsonDataService.getInstance();}
 private User actor(HttpServletRequest req){return ReviewAccess.user(data.getData(),(String)req.getSession().getAttribute("userId"));}
 private boolean allowed(User actor,Job job){
  var module=ReviewAccess.module(data.getData(),job);
  return module!=null&&Set.of("admin","organiser").contains(actor.getRole())&&ReviewAccess.course(actor,module);
 }
 private boolean canConfigure(User actor,Job job){return allowed(actor,job)&&ReviewAccess.canWrite(actor,ReviewAccess.module(data.getData(),job),ReviewAccess.CONFIGURE_COURSE);}
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{try {
   User actor=actor(req);JsonArray rows=new JsonArray();
   for(Job job:data.getData().getJobs())if(allowed(actor,job)){
    JsonObject row=JsonUtil.getGson().toJsonTree(job).getAsJsonObject();
    row.addProperty("canConfigure",canConfigure(actor,job));rows.add(row);
   }
   JsonUtil.writeJson(resp,rows);
  } catch(QualificationService.Problem e){JsonUtil.writeError(resp,e.status,e.getMessage());}}
 @Override protected void doPut(HttpServletRequest req,HttpServletResponse resp)throws IOException{
  String id=JsonUtil.getPathInfo(req);Job j=data.getData().getJobs().stream().filter(x->id.equals(x.getId())).findFirst().orElse(null);if(j==null){JsonUtil.writeError(resp,404,"Position not found.");return;}
  try{if(!canConfigure(actor(req),j))throw new QualificationService.Problem(403,"Course configuration access is required to update criteria.");JsonObject b=JsonUtil.readRequestBodyAsJson(req);String period=QualificationService.text(b,"periodId",100,true);if(data.getData().getApplicationPeriods().stream().noneMatch(p->period.equals(p.getId())))throw new QualificationService.Problem(400,"Choose an existing application cycle.");JsonObject criteria=CriteriaMatchingService.validate(b);JsonObject all=j.getPeriodCriteria()==null?new JsonObject():j.getPeriodCriteria().deepCopy();all.add(period,criteria);j.setPeriodCriteria(all);data.save();data.addLog("Matching Criteria Updated","info",(String)req.getSession().getAttribute("userName"),(String)req.getSession().getAttribute("userId"),"Updated criteria for position "+id,req.getRemoteAddr());JsonUtil.writeJson(resp,criteria);}catch(QualificationService.Problem e){JsonUtil.writeError(resp,e.status,e.getMessage());}catch(JsonParseException e){JsonUtil.writeError(resp,400,"Invalid JSON.");}
 }
}
