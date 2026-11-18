/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.trainee.details.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A deserializer that reads one item past a limit and skips the rest, so an oversized list fails
 * its size check without every item being built.
 *
 * @param <T> The type of the list's items.
 */
public abstract class CappedListDeserializer<T> extends StdDeserializer<List<T>> {

  private final Class<T> itemType;
  private final int maxItems;

  /**
   * Create a deserializer for a list with a limit.
   *
   * @param itemType The type of the list's items.
   * @param maxItems The most items the list can have.
   */
  protected CappedListDeserializer(Class<T> itemType, int maxItems) {
    super(List.class);
    this.itemType = itemType;
    this.maxItems = maxItems;
  }

  @Override
  public List<T> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    if (!parser.isExpectedStartArrayToken()) {
      throw context.wrongTokenException(parser, List.class, JsonToken.START_ARRAY, null);
    }

    List<T> items = new ArrayList<>();
    JsonToken token;

    while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
      if (items.size() > maxItems) {
        parser.skipChildren();
      } else if (token == JsonToken.VALUE_NULL) {
        items.add(null);
      } else {
        try {
          items.add(context.readValue(parser, itemType));
        } catch (JsonMappingException e) {
          throw JsonMappingException.wrapWithPath(e, items, items.size());
        }
      }
    }
    return items;
  }
}
