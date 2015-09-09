package com.maortoubian.aroundme;

/**
 * the message type that will be shown an the adapter
 */
public class Msg {

        private String email;
        private String message;
        private String name;
        private  String hour;
        private  String date;
        private String fromHow;

        public Msg(String email,String message,String date,String fromhow) {
            this.email = email;
            this.message = message;
            this.date=date;
            this.fromHow=fromhow;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setHeure(String heure) {
            this.hour = heure;
        }

        public String getHeure() {
            return hour ;
        }

        public String getDate() {
            return date ;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getFromHow() {
            return fromHow;
        }

        public void setFromHow(String fromHow) {
            this.fromHow = fromHow;
        }

    }






