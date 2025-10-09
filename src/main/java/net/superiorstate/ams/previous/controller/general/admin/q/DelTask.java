package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.GenSeq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "DelTask", value = "/DelTask")
public class DelTask extends HttpServlet {
    private RequestDispatcher dispatcher;
    public RequestDispatcher getDispatcher() {
        return dispatcher;
    }
    public void setDispatcher(RequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setDispatcher(request.getRequestDispatcher("/WEB-INF/view/checklist/checklistBuilder.jsp"));
        String tid= request.getParameter("tid");
        String ad = request.getParameter("ad");
        int tId = Integer.parseInt(tid);
        List<GenSeq> startList = (List<GenSeq>) request.getSession().getAttribute("listBuilder");
        List<GenSeq> endList = new ArrayList<>();
        if(ad.equals("d")){
            for(int i = 0; i < startList.size();i++){
                if(i!=tId)
                    endList.add(startList.get(i));
            }
            for(int j = 0; j < endList.size();j++)
                endList.get(j).setSequenceNumber(j);
        } else {
            for(int i = 0; i < tId+1;i++)
                endList.add(startList.get(i));

            GenSeq gs = new GenSeq();
            gs.setPublicTask(false);
            gs.setSequenceNumber(tId+1);
            gs.setDescription("");
            endList.add(gs);

            for(int j = tId + 1; j < startList.size(); j++)
                endList.add(startList.get(j));
        }

        request.getSession().setAttribute("listBuilder",endList);
        getDispatcher().forward(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("TaskBuilder");
        dispatcher.forward(request,response);
    }
}
