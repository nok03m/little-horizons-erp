package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.SchoolEventDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;

import java.util.List;
import java.util.Optional;

public class ExtracurricularController {

    private final ClubDao clubDao;
    private final ClubMembershipDao clubMembershipDao;
    private final SchoolEventDao schoolEventDao;
    private final EventParticipantDao eventParticipantDao;

    public ExtracurricularController(ClubDao clubDao,
                                     ClubMembershipDao clubMembershipDao,
                                     SchoolEventDao schoolEventDao,
                                     EventParticipantDao eventParticipantDao) {
        this.clubDao = clubDao;
        this.clubMembershipDao = clubMembershipDao;
        this.schoolEventDao = schoolEventDao;
        this.eventParticipantDao = eventParticipantDao;
    }

    public List<Club> listClubs() {
        return clubDao.findAll();
    }

    public Optional<Club> getClub(int id) {
        return clubDao.findById(id);
    }

    public int createClub(Club club) {
        return clubDao.insert(club);
    }

    public boolean updateClub(Club club) {
        return clubDao.update(club);
    }

    public boolean deleteClub(int id) {
        return clubDao.deleteById(id);
    }

    public List<ClubMembership> listMemberships() {
        return clubMembershipDao.findAll();
    }

    public List<ClubMembership> listMembershipsByClub(int clubId) {
        return clubMembershipDao.findByClubId(clubId);
    }

    public List<ClubMembership> listMembershipsByStudent(int studentId) {
        return clubMembershipDao.findByStudentId(studentId);
    }

    public Optional<ClubMembership> getMembership(int id) {
        return clubMembershipDao.findById(id);
    }

    public int enrollInClub(ClubMembership membership) {
        return clubMembershipDao.insertWithCapacityCheck(membership);
    }

    public boolean updateMembership(ClubMembership membership) {
        return clubMembershipDao.update(membership);
    }

    public boolean deleteMembership(int id) {
        return clubMembershipDao.deleteById(id);
    }

    public List<SchoolEvent> listEvents() {
        return schoolEventDao.findAll();
    }

    public Optional<SchoolEvent> getEvent(int id) {
        return schoolEventDao.findById(id);
    }

    public int createEvent(SchoolEvent event) {
        return schoolEventDao.insert(event);
    }

    public boolean updateEvent(SchoolEvent event) {
        return schoolEventDao.update(event);
    }

    public boolean deleteEvent(int id) {
        return schoolEventDao.deleteById(id);
    }

    public List<EventParticipant> listParticipants() {
        return eventParticipantDao.findAll();
    }

    public List<EventParticipant> listParticipantsByEvent(int eventId) {
        return eventParticipantDao.findByEventId(eventId);
    }

    public List<EventParticipant> listParticipantsByStudent(int studentId) {
        return eventParticipantDao.findByStudentId(studentId);
    }

    public Optional<EventParticipant> getParticipant(int id) {
        return eventParticipantDao.findById(id);
    }

    public int registerParticipant(EventParticipant participant) {
        return eventParticipantDao.insert(participant);
    }

    public boolean updateParticipant(EventParticipant participant) {
        return eventParticipantDao.update(participant);
    }

    public boolean deleteParticipant(int id) {
        return eventParticipantDao.deleteById(id);
    }
}
