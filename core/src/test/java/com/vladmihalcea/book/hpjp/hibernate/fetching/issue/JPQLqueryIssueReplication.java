package com.vladmihalcea.book.hpjp.hibernate.fetching.issue;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class JPQLqueryIssueReplication extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Person.class,
                Passport.class,
                Profile.class,
                Vehicle.class,
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.SHOW_SQL, "true");
        properties.setProperty(AvailableSettings.FORMAT_SQL, "true");
    }

    @Test
    public void performOperationToReplicateIssue() {
        doInJPA(entityManager -> {
            Person person = new Person();
            person.setName("Prajyot Lawande");

            Passport passport = new Passport();
            passport.setNumber("L4846508");
            passport.setIssuedOn(LocalDate.now());
            passport.setValidTill(LocalDate.now());
            passport.setPerson(person);
            person.setPassport(passport);

            Profile profile = new Profile();
            profile.setCity("Bangalore");
            profile.setPhoneNo("55-11-1116");
            profile.setPerson(person);
            person.setProfile(profile);

            Vehicle vehicle1 = new Vehicle();
            vehicle1.setNumber("GA-08 K 3369");
            vehicle1.setType("car");
            vehicle1.setPerson(person);
            Vehicle vehicle2 = new Vehicle();
            vehicle2.setNumber("GA-08 K 5564");
            vehicle2.setType("moto-scooter");
            vehicle2.setPerson(person);
            List<Vehicle> vehicles = new ArrayList<>();
            vehicles.add(vehicle1);
            vehicles.add(vehicle2);
            person.setVehicles(vehicles);

            entityManager.persist(person);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Fetching Person");
            Person person = entityManager.createQuery("""
                    select p
                    from Person p
                    left join fetch p.vehicles
                    where p.id = :id
                    """, Person.class)
                    .setParameter("id", 1L)
                    .getSingleResult();

            assertNotNull(person);
        });
    }

    @Entity(name = "Person")
    @Table(name="person_base")
    public static class Person {
        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        @Column(name="id")
        private Long id;

        @Column(name="name")
        private String name;

        @OneToOne(mappedBy="person", cascade=CascadeType.ALL)
        private Passport passport;

        @OneToOne(mappedBy="person", cascade=CascadeType.ALL)
        private Profile profile;

        @OneToMany(mappedBy="person", cascade=CascadeType.ALL, orphanRemoval = true)
        private List<Vehicle> vehicles;

        public Person() {
        }

        public Person(Long id, String name, Passport passport, Profile profile, List<Vehicle> vehicles) {
            this.id = id;
            this.name = name;
            this.passport = passport;
            this.profile = profile;
            this.vehicles = vehicles;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Passport getPassport() {
            return passport;
        }

        public void setPassport(Passport passport) {
            this.passport = passport;
        }

        public Profile getProfile() {
            return profile;
        }

        public void setProfile(Profile profile) {
            this.profile = profile;
        }

        public List<Vehicle> getVehicles() {
            return vehicles;
        }

        public void setVehicles(List<Vehicle> vehicles) {
            this.vehicles = vehicles;
        }
    }

    @Entity(name = "Passport")
    @Table(name="person_passport")
    public static class Passport {
        @Id
        @GeneratedValue(strategy=GenerationType.IDENTITY)
        @Column(name="id")
        private Long id;

        @Column(name="number")
        private String number;

        @Column(name="issued_on")
        private LocalDate issuedOn;

        @Column(name="valid_till")
        private LocalDate validTill;

        @OneToOne(fetch=FetchType.LAZY)
        @JoinColumn(name="person_id")
        private Person person;

        public Passport() {
        }

        public Passport(Long id, String number, LocalDate issuedOn, LocalDate validTill, Person person) {
            this.id = id;
            this.number = number;
            this.issuedOn = issuedOn;
            this.validTill = validTill;
            this.person = person;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public LocalDate getIssuedOn() {
            return issuedOn;
        }

        public void setIssuedOn(LocalDate issuedOn) {
            this.issuedOn = issuedOn;
        }

        public LocalDate getValidTill() {
            return validTill;
        }

        public void setValidTill(LocalDate validTill) {
            this.validTill = validTill;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }

        @Override
        public String toString() {
            return "Passport [id=" + id + ", number=" + number + ", issuedOn=" + issuedOn
                    + ", validTill=" + validTill + "]";
        }
    }

    @Entity(name = "Profile")
    @Table(name="person_profile")
    public static class Profile {
        @Id
        @Column(name="person_id")
        private Long id;

        @Column(name="city")
        private String city;

        @Column(name="phone_no")
        private String phoneNo;

        @OneToOne(fetch=FetchType.LAZY)
        @MapsId
        private Person person;

        public Profile() {
        }

        public Profile(Long id, String city, String phoneNo, Person person) {
            this.id = id;
            this.city = city;
            this.phoneNo = phoneNo;
            this.person = person;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getPhoneNo() {
            return phoneNo;
        }

        public void setPhoneNo(String phoneNo) {
            this.phoneNo = phoneNo;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }

        @Override
        public String toString() {
            return "Profile [id=" + id + ", city=" + city + ", phoneNo=" + phoneNo + "]";
        }
    }

    @Entity(name = "Vehicle")
    @Table(name="person_vehicle")
    public static class Vehicle {
        @Id
        @GeneratedValue(strategy=GenerationType.IDENTITY)
        @Column(name="id")
        private Long id;

        @Column(name="number")
        private String number;

        @Column(name="type")
        private String type;

        @ManyToOne(fetch=FetchType.LAZY)
        @JoinColumn(name="person_id")
        private Person person;

        public Vehicle() {
        }

        public Vehicle(Long id, String number, String type, Person person) {
            this.id = id;
            this.number = number;
            this.type = type;
            this.person = person;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }

        @Override
        public String toString() {
            return "Vehicle [id=" + id + ", number=" + number + ", type=" + type + "]";
        }
    }
}
