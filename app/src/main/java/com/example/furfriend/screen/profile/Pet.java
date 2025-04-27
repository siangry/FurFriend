package com.example.furfriend.screen.profile;

public class Pet {
    private String name;
    private String type;
    private String age;
    private String gender;
    private String petImage;

    public Pet() {
    }

    public Pet(String name, String type, String age, String gender, String petImage) {
        this.name = name;
        this.type = type;
        this.age = age;
        this.gender = gender;
        this.petImage = petImage;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getPetImage() {
        return petImage;
    }
}
