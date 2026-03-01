package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.controller.authentication.HelpUserLogin;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AuthDAO {
    public static String generatePasswordHash(String passwordToHash, String salt) throws NoSuchAlgorithmException {
        String generatedHash;
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt.getBytes());
        byte[] bytes = md.digest(passwordToHash.getBytes());
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < bytes.length; i++){
            sb.append(Integer.toString((bytes[i] & 0xff)+ 0x100,16).substring(1));
        }
        generatedHash = sb.toString();
        return generatedHash;
    }
    public static boolean validateLogin(EntityManager em, String userName, String passWord) throws NoSuchAlgorithmException {
        if(!validUserName(em,userName))
            return false;
        return validPassword(em, userName, passWord);
    }

    public static boolean validPassword(EntityManager em, String userName, String passWord) throws NoSuchAlgorithmException {
        User user = getUserByUserName(em,userName);
        String salt = user.getSalt();
        String generatedHash = AuthDAO.generatePasswordHash(passWord,salt);
        return generatedHash.compareTo(user.getPasswordHash()) == 0;
    }
    public static boolean validUserName(EntityManager em, String username){
        boolean isValid = getUserByUserName(em, username).getUserName() != null && getUserByUserName(em, username).getUserName() != "";
        return isValid;
    }

    public static List<User> getPspStaff(EntityManager em, PSP psp){
        Query q = em.createQuery("SELECT u FROM User u WHERE u.person.psp.id = :id");
        q.setParameter("id",psp.getId());
        List<User> fullUserList;
        try{
            fullUserList = (List<User>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            fullUserList = new ArrayList<>();
        }
        List<User> staffList = new ArrayList<>();
        for(User u: fullUserList){
            if(!u.isActive()) continue;
            List<UserRole> usersRoles = u.getUserRoleList();
            if(usersRoles.contains(getUserRoleById(em,1))||usersRoles.contains(getUserRoleById(em,5))){
                if(!staffList.contains(u))
                    staffList.add(u);
            }
        }
        return staffList;
    }

    public static String generateSalt() throws NoSuchAlgorithmException{
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt.toString();
    }

    public static UserRole getUserRoleById(EntityManager em, int id){
        Query q = em.createQuery("SELECT ur FROM UserRole ur WHERE ur.id = :id");
        q.setParameter("id",id);
        UserRole userRole;
        try{
            userRole = (UserRole) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            userRole = new UserRole();
        }
        return userRole;
    }

    public static void assignUserRoles(HttpServletRequest request, User user){
        boolean isAgent = false, isPspUser = false, isPspAdmin = false, isApplicant = false,
                isClient = false, isAgencyAdmin = false, isPspSales = false,
                isBpo = false, isBpoAdmin = false, isBpoUser = false;
        try{
            List<UserRole> userRoleList = user.getUserRoleList();
            for(UserRole ur:userRoleList){
                switch (ur.getId()){
                    case 1: isPspUser=true;break;
                    case 2: isAgent=true;break;
                    case 3: isClient=true;break;
                    case 4: isApplicant=true;break;
                    case 5: isPspAdmin=true;break;
                    case 8: isAgencyAdmin=true;break;
                    case 9: isPspSales=true;break;
                    case 101: isBpo=true;break;
                    case 102: isBpoAdmin=true;break;
                    case 103: isBpoUser=true;break;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        request.getSession().setAttribute("isAgent",isAgent);
        request.getSession().setAttribute("isPspUser",isPspUser);
        request.getSession().setAttribute("isClient",isClient);
        request.getSession().setAttribute("isApplicant",isApplicant);
        request.getSession().setAttribute("isPspAdmin",isPspAdmin);
        request.getSession().setAttribute("isAgencyAdmin",isAgencyAdmin);
        request.getSession().setAttribute("isPspSales",isPspSales);
        request.getSession().setAttribute("isBpo",isBpo);
        request.getSession().setAttribute("isBpoAdmin",isBpoAdmin);
        request.getSession().setAttribute("isBpoUser",isBpoUser);
    }


    public static User getUserByUserName(EntityManager em, String userName){
        Query q = em.createQuery("SELECT u FROM User u WHERE u.userName = :uname");
        q.setParameter("uname",userName);
        User user;
        try{
            user = (User) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            user = getUserByEmail(em,userName);
        }
        return user;
    }

    private static User getUserByEmail(EntityManager em, String email){
        Query q = em.createQuery("SELECT u FROM User u WHERE u.email = :email");
        q.setParameter("email",email);
        User user;
        try{
            user = (User) q.getSingleResult();
            return user;
        } catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
    }

    public static Person getPersonByUser(EntityManager em, User user){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.id = :id");
        q.setParameter("id",user.getPerson().getId());
        Person person;
        try{
            person = (Person) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            person = new Person();
        }
        return person;
    }


    public static User createUser(EntityManager em, Person p) throws NoSuchAlgorithmException {
        if(getUserFromPerson(em,p)!=null)
            return getUserFromPerson(em,p);
        if(getUserByUserName(em,p.getEmail())!=null)
            return getUserByUserName(em,p.getEmail());
        em.getTransaction().begin();
        User u = new User();
        u.setUserName(p.getEmail());
        u.setEmail(p.getEmail());
        u.setPerson(p);
        u.setAllowSetPassword(false);
        String salt = generateSalt();
        u.setSalt(salt);
        u.setEmailVerified(true);
        u.setGuidExpiration(Date.valueOf(LocalDate.now()));
        String tempGuid = UUID.randomUUID().toString();
        u.setTempGuid(tempGuid);
        u.setGuidUsed(true);
        u.setPasswordHash(generatePasswordHash(tempGuid,salt));
        em.persist(u);
        em.getTransaction().commit();
        UserRole ur = getUserRoleById(em,3);
        em.getTransaction().begin();
        u.getUserRoleList().add(ur);
        em.persist(u);
        em.getTransaction().commit();
        HelpUserLogin.updateUserData(em,u,true,tempGuid);
        return u;
    }

    public static User getUserFromPerson(EntityManager em, Person p){
        Query q = em.createQuery("SELECT u FROM User u WHERE u.person.id = :id");
        q.setParameter("id",p.getId());
        User u;
        try{
            u = (User) q.getSingleResult();
        } catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
        return u;
    }
    public static Person createPersonFromEmployee(EntityManager em, Employee e, PSP psp){
        if(getPersonFromEmployee(em,e)!=null)
            return getPersonFromEmployee(em,e);
        em.getTransaction().begin();
        Address a = new Address();
        a.setAddress1(e.getAddress1());
        a.setAddress2(e.getAddress2());
        a.setCity(e.getCity());
        a.setState(e.getState().substring(0,2));
        a.setZipCode(e.getZipCode());
        em.persist(a);
        em.getTransaction().commit();
        em.getTransaction().begin();
        Person p = new Person();
        p.setEmployee(e);
        p.setFirstName(e.getFirstName());
        p.setLastName(e.getLastName());
        p.setEmail(e.getEmail());
        p.setAddress(a);
        p.setPsp(psp);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    public static Person getPersonFromEmployee(EntityManager em, Employee e){
        Query q = em.createQuery("SELECT p FROM Person p WHERE p.employee.id = :id");
        q.setParameter("id",e.getId());
        Person person;
        try{
            person = (Person) q.getSingleResult();
        } catch (NoResultException exception){
            exception.printStackTrace();
            return null;
        }
        return person;
    }


}
