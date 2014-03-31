/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This code was generated by https://code.google.com/p/google-apis-client-generator/
 * (build: 2014-02-14 18:40:25 UTC)
 * on 2014-03-31 at 16:07:47 UTC 
 * Modify at your own risk.
 */

package entity.model;

/**
 * Model definition for Spots.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the rmsendpoint. For a detailed explanation see:
 * <a href="http://code.google.com/p/google-http-java-client/wiki/JSON">http://code.google.com/p/google-http-java-client/wiki/JSON</a>
 * </p>
 *
 * @author Google, Inc.
 */
@SuppressWarnings("javadoc")
public final class Spots extends com.google.api.client.json.GenericJson {

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String description;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Boolean favorite;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Boolean hasScore;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key @com.google.api.client.json.JsonString
  private java.lang.Long id;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Double latitude;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Double longitude;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String name;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Integer nbNote;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Integer score;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Float totalNote;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Integer type;

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getDescription() {
    return description;
  }

  /**
   * @param description description or {@code null} for none
   */
  public Spots setDescription(java.lang.String description) {
    this.description = description;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Boolean getFavorite() {
    return favorite;
  }

  /**
   * @param favorite favorite or {@code null} for none
   */
  public Spots setFavorite(java.lang.Boolean favorite) {
    this.favorite = favorite;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Boolean getHasScore() {
    return hasScore;
  }

  /**
   * @param hasScore hasScore or {@code null} for none
   */
  public Spots setHasScore(java.lang.Boolean hasScore) {
    this.hasScore = hasScore;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Long getId() {
    return id;
  }

  /**
   * @param id id or {@code null} for none
   */
  public Spots setId(java.lang.Long id) {
    this.id = id;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Double getLatitude() {
    return latitude;
  }

  /**
   * @param latitude latitude or {@code null} for none
   */
  public Spots setLatitude(java.lang.Double latitude) {
    this.latitude = latitude;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Double getLongitude() {
    return longitude;
  }

  /**
   * @param longitude longitude or {@code null} for none
   */
  public Spots setLongitude(java.lang.Double longitude) {
    this.longitude = longitude;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getName() {
    return name;
  }

  /**
   * @param name name or {@code null} for none
   */
  public Spots setName(java.lang.String name) {
    this.name = name;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Integer getNbNote() {
    return nbNote;
  }

  /**
   * @param nbNote nbNote or {@code null} for none
   */
  public Spots setNbNote(java.lang.Integer nbNote) {
    this.nbNote = nbNote;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Integer getScore() {
    return score;
  }

  /**
   * @param score score or {@code null} for none
   */
  public Spots setScore(java.lang.Integer score) {
    this.score = score;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Float getTotalNote() {
    return totalNote;
  }

  /**
   * @param totalNote totalNote or {@code null} for none
   */
  public Spots setTotalNote(java.lang.Float totalNote) {
    this.totalNote = totalNote;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.Integer getType() {
    return type;
  }

  /**
   * @param type type or {@code null} for none
   */
  public Spots setType(java.lang.Integer type) {
    this.type = type;
    return this;
  }

  @Override
  public Spots set(String fieldName, Object value) {
    return (Spots) super.set(fieldName, value);
  }

  @Override
  public Spots clone() {
    return (Spots) super.clone();
  }

}
